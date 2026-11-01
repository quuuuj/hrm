package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.ChunkDraft;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import com.qiujie.knowledge.service.KnowledgeParserService;
import com.qiujie.knowledge.service.TextCleanupService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ETL 阶段：取文件 → 解析 → 清洗 → 切片 → 向量化 → 落库。
 * <p>
 * 全部外部依赖经 port 接口（ObjectStore/ChunkVectorStore），嵌入直接使用 DashScopeEmbeddingClient，
 * 业务纯函数件（解析/清洗/切片）直接注入。切片原文与向量同表同批写入，产物为不可变记录。
 */
final class EtlPhase {

    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;
    private final DashScopeEmbeddingClient embeddingClient;
    private final KnowledgeParserService parserService;
    private final TextCleanupService textCleanupService;
    private final ChunkService chunkService;

    EtlPhase(ChunkVectorStore chunkVectorStore,
             ObjectStore objectStore,
             DashScopeEmbeddingClient embeddingClient,
             KnowledgeParserService parserService,
             TextCleanupService textCleanupService,
             ChunkService chunkService) {
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
        this.embeddingClient = embeddingClient;
        this.parserService = parserService;
        this.textCleanupService = textCleanupService;
        this.chunkService = chunkService;
    }

    /** ETL 产物：结算阶段（SettlePhase）所需的全部字段。 */
    record Extracted(String fullText, int chunkCount, int vectorCount) {}

    Extracted execute(ClaimPhase.Claimed claimed) {
        var doc = claimed.doc();

        // 清理优先：PG 侧自动提交不可回滚，先删旧产物保证重入无重复
        chunkVectorStore.deleteChunks(doc.getId().longValue());

        String fullText;
        try (InputStream input = objectStore.getObject(doc.getName())) {
            fullText = textCleanupService.clean(parserService.parse(input, doc.getType()));
        } catch (IOException e) {
            // 仅受检异常需要解包；运行时异常原样抛出，保持顶层 markFailed 记录原始消息
            throw new UncheckedIOException(e);
        }

        List<ChunkService.ChunkResult> chunks = chunkService.split(fullText);

        // 向量化先于落库：embedding 随切片行一起写入（失败条目 null 容忍，
        // 行仍然落库——切片可查、关键词检索可用，只是不参与向量检索）
        List<float[]> vectors = embeddingClient.embed(
                chunks.stream().map(ChunkService.ChunkResult::getText).toList());
        List<ChunkDraft> drafts = new ArrayList<>();
        int vectorCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            ChunkService.ChunkResult cr = chunks.get(i);
            float[] vec = vectors.get(i);
            if (vec != null) {
                vectorCount++;
            }
            drafts.add(new ChunkDraft(i, cr.getText(), cr.getTokenCount(),
                    cr.getCharStart(), cr.getCharEnd(), vec));
        }
        chunkVectorStore.storeChunks(doc.getId().longValue(), doc.getOldName(), drafts);

        return new Extracted(fullText, chunks.size(), vectorCount);
    }
}
