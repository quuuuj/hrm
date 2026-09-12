package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.enums.DocumentStatusEnum;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.ChunkDraft;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.ChunkRecord;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore.VectorDraft;
import com.qiujie.knowledge.lifecycle.port.EmbeddingProvider;
import com.qiujie.knowledge.lifecycle.port.ObjectStore;
import com.qiujie.knowledge.service.ChunkService;
import com.qiujie.knowledge.service.DocumentParserService;
import com.qiujie.knowledge.service.TextCleanupService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ETL 阶段：取文件 → 解析 → 清洗 → 切片 → 持久化切片 → 向量化 → 写向量 → 同步镜像。
 * <p>
 * 全部外部依赖经 port 接口（ObjectStore/EmbeddingProvider/ChunkVectorStore），
 * 业务纯函数件（解析/清洗/切片）直接注入。产物为不可变记录，结算所需字段全部显式携带。
 */
final class EtlPhase {

    private final ChunkVectorStore chunkVectorStore;
    private final ObjectStore objectStore;
    private final EmbeddingProvider embeddingProvider;
    private final DocumentParserService parserService;
    private final TextCleanupService textCleanupService;
    private final ChunkService chunkService;

    EtlPhase(ChunkVectorStore chunkVectorStore,
             ObjectStore objectStore,
             EmbeddingProvider embeddingProvider,
             DocumentParserService parserService,
             TextCleanupService textCleanupService,
             ChunkService chunkService) {
        this.chunkVectorStore = chunkVectorStore;
        this.objectStore = objectStore;
        this.embeddingProvider = embeddingProvider;
        this.parserService = parserService;
        this.textCleanupService = textCleanupService;
        this.chunkService = chunkService;
    }

    /** ETL 产物：结算阶段（SettlePhase）所需的全部字段。 */
    record Extracted(String fullText, int chunkCount, List<VectorDraft> vectorDrafts) {}

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
        List<ChunkDraft> drafts = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            ChunkService.ChunkResult cr = chunks.get(i);
            drafts.add(new ChunkDraft(i, cr.getText(), cr.getTokenCount(), cr.getCharStart(), cr.getCharEnd()));
        }
        List<ChunkRecord> records = chunkVectorStore.storeChunks(doc.getId().longValue(), drafts);

        // 向量化（失败条目 null 容忍），metadata 经 VectorDraft 单点携带
        List<float[]> vectors = embeddingProvider.embedTexts(
                records.stream().map(r -> r.draft().chunkText()).toList());
        List<VectorDraft> vectorDrafts = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            float[] vec = vectors.get(i);
            if (vec == null) {
                continue;
            }
            ChunkRecord record = records.get(i);
            vectorDrafts.add(new VectorDraft(doc.getId().longValue(), record.id(),
                    record.draft().chunkText(), doc.getOldName(), vec));
        }
        chunkVectorStore.storeVectors(vectorDrafts);

        // 同步 PG kb_document 镜像行（关键词检索/邻窗扩展的 JOIN 来源）
        chunkVectorStore.upsertDocumentMirror(doc.setKbStatus(DocumentStatusEnum.READY.name()));

        return new Extracted(fullText, chunks.size(), List.copyOf(vectorDrafts));
    }
}
