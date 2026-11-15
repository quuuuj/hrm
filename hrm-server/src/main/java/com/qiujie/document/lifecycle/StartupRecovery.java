package com.qiujie.document.lifecycle;

import com.qiujie.document.entity.Document;
import com.qiujie.document.lifecycle.port.ChunkVectorStore;
import com.qiujie.document.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 启动恢复（模块内部组件）：覆盖两个崩溃窗口。
 * <ol>
 *   <li>执行中崩溃：PROCESSING → FAILED + 原因</li>
 *   <li>未启动崩溃：UPLOADED 续跑（CAS 认领兜底并发，语义优于置失败）</li>
 * </ol>
 * 另有一步索引一致性核对（{@link #purgeOrphanIndexes}）：PG 里有切片/向量、但
 * {@code kb_document} 已无对应行的文档，其索引永远不会被检索命中——不做这一步，孤儿数据
 * 会一直堆积并挤占检索候选。删除流程崩溃留下的残留也由这一步统一收口。
 */
final class StartupRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

    private static final String INTERRUPT_REASON = "文档处理因服务中断未完成，请重试";

    private final DocumentMapper documentMapper;
    private final IngestionPipeline pipeline;
    private final ChunkVectorStore chunkVectorStore;
    private final Executor executor;

    StartupRecovery(DocumentMapper documentMapper,
                    IngestionPipeline pipeline,
                    ChunkVectorStore chunkVectorStore,
                    Executor executor) {
        this.documentMapper = documentMapper;
        this.pipeline = pipeline;
        this.chunkVectorStore = chunkVectorStore;
        this.executor = executor;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 步骤 1：快速 SQL 更新，同步执行
        int markedFailed = documentMapper.markStaleProcessingAsFailed(INTERRUPT_REASON);

        // 步骤 2-3：涉及远程调用（MinIO/PG/HTTP），异步执行，不阻塞启动
        executor.execute(() -> {
            int recovered = 0;
            List<Document> uploaded = documentMapper.selectLiveUploaded();
            for (Document document : uploaded) {
                pipeline.run(document.getId());
                recovered++;
            }
            int orphaned = purgeOrphanIndexes();
            if (markedFailed + recovered + orphaned > 0) {
                log.info("Recovered {} documents on startup (failed={}, rerun={}, orphan-indexes={})",
                        markedFailed + recovered + orphaned, markedFailed, recovered, orphaned);
            }
        });
    }

    /**
     * 清理失去主表引用的 PG 索引（切片 + 向量同表）。
     * <p>
     * 文档行已物理删除，{@code kb_document} 里查不到的 documentId 即孤儿：既不会被检索命中，
     * 也没有 delete 流程会再清理它。删除流程崩溃留下的残留在这一步统一收口。
     * </p>
     *
     * @return 清理掉的孤儿文档数
     */
    private int purgeOrphanIndexes() {
        Set<Long> indexed = chunkVectorStore.listIndexedDocumentsIds();
        if (indexed.isEmpty()) {
            return 0;
        }
        Set<Long> live = new HashSet<>(documentMapper.selectAllIds());
        int purged = 0;
        for (Long documentId : indexed) {
            if (!live.contains(documentId)) {
                chunkVectorStore.deleteChunks(documentId);
                purged++;
            }
        }
        if (purged > 0) {
            log.warn("Purged {} orphan chunk indexes without a live document row", purged);
        }
        return purged;
    }
}
