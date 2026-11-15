package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.enums.KnowledgeStatusEnum;
import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 文档生命周期门面——生命周期唯一的公共 API。
 * <p>
 * 命令方法只做两件事：事务内写状态 + 注册「事务提交后」的异步任务；
 * ETL、清理、状态机补偿全部在模块内部，调用方不感知异步、不接触线程池。
 * <p>
 * 事务边界由 {@link Transactional} 代理管理，{@link #scheduleAfterCommit} 依赖它
 * 保证"挂回调时事务已开启"：代理失效（类改回 final、方法非 public、类内自调用）
 * 会让回调在无事务上下文里立即执行，worker 读不到未提交的文档而永久卡死。
 */
@Component
public class KnowledgeLifecycleService {

    /** 上传完成登记。name = mergedKey = MinIO 对象键（物理文件引用键）。 */
    public record RegisterCommand(String name, String oldName, String type,
                                  String fileHash, Long fileSize, Integer staffId) {}

    /** 登记成功：文档 UPLOADED，ETL 将在事务提交后异步执行。 */
    public record RegisterResult(Long docsId, String status) {}

    /** 重试命令。 */
    public record RetryCommand(Long docsId) {}

    /** 重试裁决：accepted=false 时 reason 说明拒绝原因（预期场景用结果表达，不抛异常）。 */
    public record RetryResult(Long docsId, boolean accepted, String reason) {}

    /** 删除命令。 */
    public record DeleteCommand(Long docsId) {}

    /** 删除裁决：removed=true 表示本次真正删掉了行；行已不存在时 false（删两次与删不存在的 id 结果相同）。 */
    public record DeleteResult(Long docsId, boolean removed) {}

    @Autowired
    private DocsMapper docsMapper;

    @Autowired
    private KnowledgeIngestionTaskMapper taskMapper;

    @Autowired
    @Qualifier("fileTaskExecutor")
    private Executor ingestExecutor;

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private KnowledgePurgeHandler purgeHandler;

    /**
     * 上传完成登记：事务内 INSERT sys_docs(kb_status=UPLOADED)，提交后异步 ETL。
     * 事务内调用（分片上传完成回调）加入外层事务、事务外调用（补发摄入）自开事务。
     */
    @Transactional(rollbackFor = Exception.class)
    public RegisterResult register(RegisterCommand cmd) {
        Docs doc = new Docs()
                .setName(cmd.name())
                .setOldName(cmd.oldName())
                .setType(cmd.type())
                .setFileHash(cmd.fileHash())
                .setStoredSize(cmd.fileSize())
                .setSize(cmd.fileSize() != null ? cmd.fileSize() / 1024 : null)
                .setKbStatus(KnowledgeStatusEnum.UPLOADED.name())
                .setStaffId(cmd.staffId())
                .setUploadTime(LocalDateTime.now());
        docsMapper.insert(doc);
        Long docsId = doc.getId().longValue();
        scheduleAfterCommit(() -> pipeline.run(docsId));
        return new RegisterResult(docsId, KnowledgeStatusEnum.UPLOADED.name());
    }

    /**
     * 重试摄入：READY/已删除/不存在拒绝（accepted=false + reason）；
     * UPLOADED/FAILED/PROCESSING 接受，提交后异步 ETL（并发互斥由管道内 CAS 认领兜底）。
     */
    @Transactional(rollbackFor = Exception.class)
    public RetryResult retry(RetryCommand cmd) {
        Docs doc = docsMapper.selectById(cmd.docsId());
        if (doc == null) {
            return new RetryResult(cmd.docsId(), false, "文档不存在或已删除");
        }
        if (KnowledgeStatusEnum.READY.name().equals(doc.getKbStatus())) {
            return new RetryResult(cmd.docsId(), false, "已处理完成的文档无需重试");
        }
        Long docsId = doc.getId().longValue();
        scheduleAfterCommit(() -> pipeline.run(docsId));
        return new RetryResult(docsId, true, null);
    }

    /**
     * 删除文档（幂等）：事务内物理删行 + 作废在途任务，提交后异步清理物理产物
     * （MinIO 对象 → PG 切片/向量）。
     * <p>
     * 行删除即失去对象键线索，因此对象键必须在事务内随行取出、交给提交后任务。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public DeleteResult delete(DeleteCommand cmd) {
        Docs doc = docsMapper.selectById(cmd.docsId());
        if (doc == null || docsMapper.deleteById(cmd.docsId()) == 0) {
            return new DeleteResult(cmd.docsId(), false);
        }
        // 作废在途任务，防 worker 在删除后继续写 PG
        taskMapper.cancelActiveTasks(cmd.docsId());
        String storageKey = doc.getName();
        scheduleAfterCommit(() -> purgeHandler.purge(cmd.docsId(), storageKey));
        return new DeleteResult(cmd.docsId(), true);
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
