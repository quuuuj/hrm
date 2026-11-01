package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import com.qiujie.knowledge.service.KnowledgeParserService;
import com.qiujie.knowledge.service.TextCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ETL 摄入管道（模块内部组件，由 {@link KnowledgeLifecycleService} 在事务提交后触发）。
 * <p>
 * 仅编排三个有序 Phase，不含任何步骤业务逻辑：
 * <ol>
 *   <li>{@link ClaimPhase} — 文档加载 + CAS 认领 + 建任务</li>
 *   <li>{@link EtlPhase} — 取文件 → 解析 → 清洗 → 切片 → 向量化 → 落库（存储经 port，嵌入直连 Client）</li>
 *   <li>{@link SettlePhase} — READY / CANCELLED / FAILED 状态机结算 + 失败补偿</li>
 * </ol>
 * 失败语义：任一 Phase 抛出即进入统一失败结算（任务 FAILED + 文档 FAILED + 原因）。
 */
public final class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    private final ClaimPhase claimPhase;
    private final EtlPhase etlPhase;
    private final SettlePhase settlePhase;

    public IngestionPipeline(DocsMapper docsMapper,
                             KnowledgeIngestionTaskMapper taskMapper,
                             ChunkVectorStore chunkVectorStore,
                             ObjectStore objectStore,
                             DashScopeEmbeddingClient embeddingClient,
                             KnowledgeParserService parserService,
                             TextCleanupService textCleanupService,
                             ChunkService chunkService) {
        this.claimPhase = new ClaimPhase(docsMapper, taskMapper);
        this.etlPhase = new EtlPhase(chunkVectorStore, objectStore, embeddingClient,
                parserService, textCleanupService, chunkService);
        this.settlePhase = new SettlePhase(docsMapper, taskMapper, chunkVectorStore);
    }

    /** 执行摄入（工作线程调用，无 Spring 事务；PG 侧 best-effort）。 */
    void run(Long docsId) {
        var claimed = claimPhase.execute(docsId);
        if (claimed.isEmpty()) {
            log.debug("Ingestion skipped (not claimable): docsId={}", docsId);
            return;
        }
        try {
            var extracted = etlPhase.execute(claimed.get());
            settlePhase.succeed(claimed.get(), extracted);
            log.info("ETL completed: docsId={}, chunks={}, vectors={}",
                    docsId, extracted.chunkCount(), extracted.vectorCount());
        } catch (Exception e) {
            log.error("ETL failed: docsId={}", docsId, e);
            settlePhase.fail(claimed.get(), e);
        }
    }
}
