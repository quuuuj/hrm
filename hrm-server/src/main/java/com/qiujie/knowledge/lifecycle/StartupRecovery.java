package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * 启动恢复（模块内部组件）：覆盖四个崩溃窗口。
 * <ol>
 *   <li>遗留 RUNNING 作业 → FAILED（收尾）</li>
 *   <li>执行中崩溃：PROCESSING → FAILED + 原因</li>
 *   <li>未启动崩溃：UPLOADED 续跑（CAS 认领兜底并发，语义优于置失败）</li>
 *   <li>已删孤儿重 purge（覆盖"逻辑删提交后、清理执行前"崩溃窗口，幂等）</li>
 * </ol>
 * 另有一步索引一致性核对（{@link #purgeOrphanIndexes}）：PG 里有切片/向量、但
 * {@code sys_docs} 已无对应行的文档，其索引永远不会被检索命中，也永远不会被
 * delete 流程清理——不做这一步，孤儿数据会一直堆积并挤占检索候选。
 */
final class StartupRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupRecovery.class);

    private static final String INTERRUPT_REASON = "文档处理因服务中断未完成，请重试";

    private final DocsMapper documentMapper;
    private final IngestionJobMapper jobMapper;
    private final IngestionPipeline pipeline;
    private final DocumentPurgeHandler purgeHandler;
    private final ChunkVectorStore chunkVectorStore;
    private final Executor executor;

    StartupRecovery(DocsMapper documentMapper,
                    IngestionJobMapper jobMapper,
                    IngestionPipeline pipeline,
                    DocumentPurgeHandler purgeHandler,
                    ChunkVectorStore chunkVectorStore,
                    Executor executor) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
        this.pipeline = pipeline;
        this.purgeHandler = purgeHandler;
        this.chunkVectorStore = chunkVectorStore;
        this.executor = executor;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 步骤 1-2：快速 SQL 更新，同步执行
        jobMapper.markStaleRunningAsFailed(INTERRUPT_REASON);
        int markedFailed = documentMapper.markStaleProcessingAsFailed(INTERRUPT_REASON);

        // 步骤 3-5：涉及远程调用（MinIO/PG/HTTP），异步执行，不阻塞启动
        executor.execute(() -> {
            int recovered = 0;
            List<Docs> uploaded = documentMapper.selectLiveUploaded();
            for (Docs doc : uploaded) {
                pipeline.run(doc.getId().longValue());
                recovered++;
            }
            List<Docs> deleted = documentMapper.selectDeleted();
            for (Docs doc : deleted) {
                purgeHandler.purge(doc.getId().longValue(), doc.getName());
                recovered++;
            }
            int orphaned = purgeOrphanIndexes();
            if (markedFailed + recovered + orphaned > 0) {
                log.info("Recovered {} documents on startup (failed={}, rerun/purged={}, orphan-indexes={})",
                        markedFailed + recovered + orphaned, markedFailed, recovered, orphaned);
            }
        });
    }

    /**
     * 清理失去主表引用的 PG 索引（切片 + 向量 + 镜像）。
     * <p>
     * 只删「MySQL 里完全没有该 id 行」的文档：逻辑删除的行仍在表里，由上面的
     * purge 步骤负责，不能在这里重复处理，更不能据此判断为孤儿。
     * </p>
     *
     * @return 清理掉的孤儿文档数
     */
    private int purgeOrphanIndexes() {
        try {
            Set<Long> known = new HashSet<>(documentMapper.selectAllIds());
            int purged = 0;
            for (Long documentId : chunkVectorStore.listIndexedDocumentIds()) {
                if (known.contains(documentId)) {
                    continue;
                }
                log.warn("Purging orphan knowledge index (no sys_docs row): documentId={}", documentId);
                chunkVectorStore.deleteChunks(documentId);
                chunkVectorStore.deleteDocumentMirror(documentId);
                purged++;
            }
            return purged;
        } catch (Exception e) {
            log.warn("Orphan index reconciliation skipped: {}", e.getMessage());
            return 0;
        }
    }
}
