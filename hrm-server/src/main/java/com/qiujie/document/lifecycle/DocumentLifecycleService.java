package com.qiujie.document.lifecycle;

import com.qiujie.document.entity.Document;
import com.qiujie.document.enums.DocumentStatusEnum;
import com.qiujie.document.mapper.DocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 知识库文档生命周期门面——模块唯一的公共 API。
 * <p>
 * 命令方法只做两件事：事务内写状态 + 注册「事务提交后」的异步任务；
 * ETL、清理、状态机补偿全部在模块内部，调用方不感知异步、不接触线程池。
 * <p>
 * 事务边界由 {@link Transactional} 代理管理，{@link #scheduleAfterCommit} 依赖它
 * 保证"挂回调时事务已开启"：代理失效（类改回 final、方法非 public、类内自调用）
 * 会让回调在无事务上下文里立即执行，worker 读不到未提交的文档而永久卡死。
 */
@Component
public class DocumentLifecycleService {

    /** 上传完成登记。name = mergedKey = MinIO 对象键（物理文件引用键）。 */
    public record RegisterCommand(String name, String oldName, String type, String fileHash,
                                  Long size, Long storedSize, Integer compressed, Integer staffId) {}

    /** 登记成功：文档 UPLOADED，ETL 将在事务提交后异步执行。 */
    public record RegisterResult(Long documentId, String status) {}

    /** 重试命令。 */
    public record RetryCommand(Long documentId) {}

    /** 重试裁决：accepted=false 时 reason 说明拒绝原因（预期场景用结果表达，不抛异常）。 */
    public record RetryResult(Long documentId, boolean accepted, String reason) {}

    /** 删除命令。 */
    public record DeleteCommand(Long documentId) {}

    /** 删除裁决：removed=true 表示本次真正删掉了行；行已不存在时 false（删两次与删不存在的 id 结果相同）。 */
    public record DeleteResult(Long documentId, boolean removed) {}

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    @Qualifier("fileTaskExecutor")
    private Executor ingestExecutor;

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private DocumentPurgeHandler purgeHandler;

    /**
     * 上传完成登记：事务内 INSERT kb_document(status=UPLOADED)，提交后异步 ETL。
     * 事务内调用（分片上传完成回调）加入外层事务、事务外调用（补发摄入）自开事务。
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResult register(RegisterCommand cmd) {
        Document document = new Document()
                .setName(cmd.name())
                .setOldName(cmd.oldName())
                .setType(cmd.type())
                .setFileHash(cmd.fileHash())
                .setSize(cmd.size())
                .setStoredSize(cmd.storedSize())
                .setCompressed(cmd.compressed())
                .setStatus(DocumentStatusEnum.UPLOADED.name())
                .setStaffId(cmd.staffId())
                .setUploadTime(LocalDateTime.now());
        documentMapper.insert(document);
        Long documentId = document.getId();
        scheduleAfterCommit(() -> pipeline.run(documentId));
        return new RegisterResult(documentId, DocumentStatusEnum.UPLOADED.name());
    }

    /**
     * 重试摄入：READY/不存在拒绝（accepted=false + reason）；
     * UPLOADED/FAILED/PROCESSING 接受，提交后异步 ETL（并发互斥由管道内 CAS 认领兜底）。
     */
    @Transactional(rollbackFor = Exception.class)
    public RetryResult retry(RetryCommand cmd) {
        Document document = documentMapper.selectById(cmd.documentId());
        if (document == null) {
            return new RetryResult(cmd.documentId(), false, "文档不存在或已删除");
        }
        if (DocumentStatusEnum.READY.name().equals(document.getStatus())) {
            return new RetryResult(cmd.documentId(), false, "已处理完成的文档无需重试");
        }
        Long documentId = document.getId();
        scheduleAfterCommit(() -> pipeline.run(documentId));
        return new RetryResult(documentId, true, null);
    }

    /**
     * 删除文档（幂等）：事务内物理删行，提交后异步清理物理产物（MinIO 对象 → PG 切片/向量）。
     * <p>
     * 行删除即失去对象键线索，因此对象键必须在事务内随行取出、交给提交后任务。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DeleteResult delete(DeleteCommand cmd) {
        Document document = documentMapper.selectById(cmd.documentId());
        if (document == null || documentMapper.deleteById(cmd.documentId()) == 0) {
            return new DeleteResult(cmd.documentId(), false);
        }
        String storageKey = document.getName();
        scheduleAfterCommit(() -> purgeHandler.purge(cmd.documentId(), storageKey));
        return new DeleteResult(cmd.documentId(), true);
    }

    /**
     * 事务提交后触发 ETL。
     * <p>
     * 依赖 {@link Transactional} 代理已开启事务同步上下文（三个命令方法都经代理调用）；
     * 嵌套事务场景下同步注册在最外层事务提交后才执行——修复"new Thread 在外层事务
     * 提交前启动、worker 读不到未提交文档"的永久卡死 bug。
     * <p>
     * 同步上下文不存在时 {@link TransactionSynchronizationManager#registerSynchronization}
     * 直接抛 {@link IllegalStateException}：宁可调用点报错，也不要在提交前把 ETL 放出去。
     */
    private void scheduleAfterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ingestExecutor.execute(task);
            }
        });
    }
}
