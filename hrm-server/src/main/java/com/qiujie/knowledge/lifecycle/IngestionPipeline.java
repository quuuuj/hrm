package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DocumentParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ETL 摄入管道（模块内部组件，由 {@link DocumentLifecycleService} 在事务提交后触发）。
 * <p>
 * 仅编排三个有序 Phase，不含任何步骤业务逻辑：
 * <ol>
 *   <li>{@link ClaimPhase} — 文档加载 + CAS 认领 + 建作业</li>
 *   <li>{@link EtlPhase} — 取文件 → 解析 → 清洗 → 切片 → 持久化 → 向量化 → 写向量 → 同步镜像（全经 port）</li>
 *   <li>{@link SettlePhase} — READY / CANCELLED / FAILED 状态机结算 + 失败补偿</li>
 * </ol>
 * 失败语义：任一 Phase 抛出即进入统一失败结算（作业 FAILED + 文档 FAILED + 原因）。
 */
public final class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final ClaimPhase claimPhase;
    private final EtlPhase etlPhase;
    private final SettlePhase settlePhase;

    public IngestionPipeline(DocsMapper documentMapper,
                             IngestionJobMapper jobMapper,
                             ChunkVectorStore chunkVectorStore,
                             ObjectStore objectStore,
                             EmbeddingProvider embeddingProvider,
                             DocumentParserService parserService,
                             TextCleanupService textCleanupService,
                             ChunkService chunkService) {
        this.claimPhase = new ClaimPhase(documentMapper, jobMapper);
        this.etlPhase = new EtlPhase(chunkVectorStore, objectStore, embeddingProvider,
                parserService, textCleanupService, chunkService);
        this.settlePhase = new SettlePhase(documentMapper, jobMapper, chunkVectorStore);
    }

    /** 执行摄入（工作线程调用，无 Spring 事务；PG 侧 best-effort）。 */
    void run(Long documentId) {
        var claimed = claimPhase.execute(documentId);
        if (claimed.isEmpty()) {
            log.debug("Ingestion skipped (not claimable): documentId={}", documentId);
            return;
        }
        try {
            var extracted = etlPhase.execute(claimed.get());
            settlePhase.succeed(claimed.get(), extracted);
            log.info("ETL completed: documentId={}, chunks={}, vectors={}",
                    documentId, extracted.chunkCount(), extracted.vectorDrafts().size());
        } catch (Exception e) {
            log.error("ETL failed: documentId={}", documentId, e);
            settlePhase.fail(claimed.get(), e);
        }
    }
}
