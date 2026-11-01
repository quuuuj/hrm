package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.enums.KnowledgeStatusEnum;
import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.Executor;

/**
 * 文档生命周期门面——生命周期唯一的公共 API。
 * <p>
 * 命令方法只做两件事：事务内写状态 + 注册「事务提交后」的异步任务；
 * ETL、清理、状态机补偿全部在模块内部，调用方不感知异步、不接触线程池。
 */
public final class KnowledgeLifecycleService {

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

    /** 删除裁决：幂等——不存在/已删除 → alreadyDeleted=true 仍算成功。 */
    public record DeleteResult(Long docsId, boolean alreadyDeleted) {}

    private final DocsMapper docsMapper;
    private final KnowledgeIngestionTaskMapper taskMapper;
    private final TransactionTemplate transactionTemplate;
    private final Executor ingestExecutor;
    private final IngestionPipeline pipeline;
    private final KnowledgePurgeHandler purgeHandler;

    public KnowledgeLifecycleService(DocsMapper docsMapper,
                                    KnowledgeIngestionTaskMapper taskMapper,
                                    TransactionTemplate transactionTemplate,
                                    Executor ingestExecutor,
                                    IngestionPipeline pipeline,
                                    KnowledgePurgeHandler purgeHandler) {
        this.docsMapper = docsMapper;
        this.taskMapper = taskMapper;
        this.transactionTemplate = transactionTemplate;
        this.ingestExecutor = ingestExecutor;
        this.pipeline = pipeline;
        this.purgeHandler = purgeHandler;
    }

    /**
     * 上传完成登记：事务内 INSERT sys_docs(kb_status=UPLOADED)，提交后异步 ETL。
     * 事务内调用（分片上传完成回调）加入外层事务、事务外调用（补发摄入）自开事务。
     */
    public RegisterResult register(RegisterCommand cmd) {
        return transactionTemplate.execute(status -> {
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
        });
    }

    /**
     * 重试摄入：READY/已删除/不存在拒绝（accepted=false + reason）；
     * UPLOADED/FAILED/PROCESSING 接受，提交后异步 ETL（并发互斥由管道内 CAS 认领兜底）。
     */
    public RetryResult retry(RetryCommand cmd) {
        return transactionTemplate.execute(status -> {
            // @TableLogic：已删除行 selectById 返回 null，与"不存在"同路拒绝
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
        });
    }

    /**
     * 删除文档（幂等）：事务内 CAS 逻辑删 + 作废在途任务，提交后异步物理清理
     * （引用计数 → MinIO → PG 切片/向量/镜像）。
     */
    public DeleteResult delete(DeleteCommand cmd) {
        return transactionTemplate.execute(status -> {
            Docs doc = docsMapper.selectById(cmd.docsId());
            if (doc == null || docsMapper.markDeleted(cmd.docsId()) == 0) {
                // 不存在或已删除（@TableLogic selectById 查不到已删行）：幂等成功
                return new DeleteResult(cmd.docsId(), true);
            }
            // 作废在途任务，防 worker 在删除后继续写 PG
            taskMapper.cancelActiveTasks(cmd.docsId());
            String storageKey = doc.getName();
            scheduleAfterCommit(() -> purgeHandler.purge(cmd.docsId(), storageKey));
            return new DeleteResult(cmd.docsId(), false);
        });
    }

    /**
     * 事务提交后触发 ETL；无活动事务时立即触发（独立调用与单元测试场景）。
     * 嵌套事务场景下同步注册在最外层事务提交后才执行——修复"new Thread 在外层
     * 事务提交前启动、worker 读不到未提交文档"的永久卡死 bug。
     */
    private void scheduleAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ingestExecutor.execute(task);
                }
            });
        } else {
            ingestExecutor.execute(task);
        }
    }
}
