package com.qiujie.document.lifecycle;

import com.qiujie.document.lifecycle.port.ChunkVectorStore;
import com.qiujie.document.lifecycle.port.ObjectStore;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.document.service.ChunkService;
import com.qiujie.document.service.DashScopeEmbeddingClient;
import com.qiujie.document.service.DocumentParserService;
import com.qiujie.document.service.TextCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ETL 摄入管道（模块内部组件，由 {@link DocumentLifecycleService} 在事务提交后触发）。
 * <p>
 * 仅编排三个有序 Phase，不含任何步骤业务逻辑：
 * <ol>
 *   <li>{@link ClaimPhase} — 文档加载 + CAS 认领</li>
 *   <li>{@link EtlPhase} — 取文件 → 解析 → 清洗 → 切片 → 向量化 → 落库（存储经 port，嵌入直连 Client）</li>
 *   <li>{@link SettlePhase} — READY / FAILED 状态机结算 + 失败补偿</li>
 * </ol>
 * 失败语义：任一 Phase 抛出即进入统一失败结算（文档 FAILED + 原因）。
 */
public final class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final ClaimPhase claimPhase;
    private final EtlPhase etlPhase;
    private final SettlePhase settlePhase;

    public IngestionPipeline(DocumentMapper documentMapper,
                             ChunkVectorStore chunkVectorStore,
                             ObjectStore objectStore,
                             DashScopeEmbeddingClient embeddingClient,
                             DocumentParserService parserService,
                             TextCleanupService textCleanupService,
                             ChunkService chunkService) {
        this.claimPhase = new ClaimPhase(documentMapper);
        this.etlPhase = new EtlPhase(chunkVectorStore, objectStore, embeddingClient,
                parserService, textCleanupService, chunkService);
        this.settlePhase = new SettlePhase(documentMapper, chunkVectorStore);
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
                    documentId, extracted.chunkCount(), extracted.vectorCount());
        } catch (Exception e) {
            log.error("ETL failed: documentId={}", documentId, e);
            settlePhase.fail(claimed.get(), e);
        }
    }
}
