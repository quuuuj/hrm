package com.qiujie.document.service;

import com.qiujie.document.entity.Document;
import com.qiujie.document.entity.DocumentChunk;
import com.qiujie.common.storage.MinioStorageService;
import com.qiujie.document.mapper.DocumentChunkMapper;
import com.qiujie.document.mapper.DocumentMapper;
import com.qiujie.document.util.ChunkUtil;
import com.qiujie.document.util.DocumentParserUtil;
import com.qiujie.document.util.TextCleanupUtil;
import com.qiujie.util.StorageCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

/**
 * ETL 摄入管道（模块内部组件，由 {@link DocumentService} 在事务提交后触发；
 * 崩溃恢复走「重试」按钮重发）。
 * <p>
 * 并发互斥在调用方：{@link DocumentService} 在事务内用
 * {@link DocumentMapper#claimForProcessing} 认领，只有认领成功的调用才会推进到这里。
 * 本类因此只做「文档已归我」之后的两段：
 * <ol>
 *   <li>ETL — 读文档行 → 取文件 → 解析 → 清洗 → 切片 → 向量化 → 落库</li>
 *   <li>结算 — READY / FAILED 状态机收口 + 失败补偿</li>
 * </ol>
 * 失败语义：ETL 中任一异常即进入统一失败结算（文档 FAILED + 原因）。
 */
@Component
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);

    /** 预览与失败原因的长度上限：预览截 500 字，错误信息截 500 字防超列。 */
    private static final int PREVIEW_MAX_CHARS = 500;
    private static final int REASON_MAX_CHARS = 500;

    /** 最大切片字符数 */
    @Value("${kb.chunk.max-size}")
    private int chunkMaxSize;

    /** 相邻切片重叠字符数 */
    @Value("${kb.chunk.overlap}")
    private int chunkOverlap;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private DocumentChunkMapper chunkMapper;

    @Autowired
    private MinioStorageService storageService;

    @Autowired
    private DashScopeEmbeddingClient embeddingClient;

    /** 执行摄入（工作线程调用，无 Spring 事务；PG 侧 best-effort）。调用前文档必须已被认领为 PROCESSING。 */
    void run(Long documentId) {
        try {
            executeEtl(documentId).ifPresent(outcome -> settleSuccess(documentId, outcome));
        } catch (Exception e) {
            settleFailure(documentId, e);
        }
    }

    /**
     * ETL 段：读文档行 → 取文件 → 解析 → 清洗 → 切片 → 向量化 → 落库。
     *
     * @return 认领后文档行已被删则返回空，无主可结算，由调用方直接退出
     */
    private Optional<EtlOutcome> executeEtl(Long documentId) {
        // 认领后读文档行：行在认领与读取之间被删则无主可结算，直接退出
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            log.debug("Ingestion aborted (document removed after claim): documentId={}", documentId);
            return Optional.empty();
        }
        // 清理优先：PG 侧自动提交不可回滚，先删旧产物保证重入无重复
        chunkMapper.deleteByDocumentId(documentId);

        String fullText = readFullText(document);
        List<ChunkUtil.ChunkResult> chunks = ChunkUtil.split(fullText, chunkMaxSize, chunkOverlap);

        // 向量化先于落库：embedding 随切片行一起写入（失败条目 null 容忍，
        // 行仍然落库——切片可查、关键词检索可用，只是不参与向量检索）
        List<float[]> vectors = embeddingClient.embed(
                chunks.stream().map(ChunkUtil.ChunkResult::text).toList());
        String documentName = document.getOldName() == null ? "" : document.getOldName();
        int vectorCount = 0;
        for (int i = 0; i < chunks.size(); i++) {
            ChunkUtil.ChunkResult cr = chunks.get(i);
            float[] vec = vectors.get(i);
            if (vec != null) {
                vectorCount++;
            }
            chunkMapper.insert(new DocumentChunk()
                    .setDocumentId(documentId)
                    .setDocumentName(documentName)
                    .setChunkIndex(i)
                    .setChunkText(cr.text())
                    .setTokenCount(cr.tokenCount())
                    .setCharStart(cr.charStart())
                    .setCharEnd(cr.charEnd()), vec);
        }

        return Optional.of(new EtlOutcome(fullText, chunks.size(), vectorCount));
    }

    /** 取对象存储中的源文件，按需解压后解析清洗成可落库的纯文本。 */
    private String readFullText(Document document) {
        try (InputStream raw = storageService.get(document.getName())) {
            // 魔数判断解压，与下载链路一致；txt 上传时会被 zstd 压缩后覆盖原对象
            byte[] bytes = StorageCompressor.decompressIfCompressed(raw.readAllBytes());
            return TextCleanupUtil.clean(
                    DocumentParserUtil.parse(new ByteArrayInputStream(bytes), document.getType()));
        } catch (IOException e) {
            // 仅受检异常需要解包；运行时异常原样抛出，保持顶层失败结算记录原始消息
            throw new UncheckedIOException(e);
        }
    }

    /** 结算成功段：PROCESSING → READY + 预览/切片数；执行中被删则清理残留。 */
    private void settleSuccess(Long documentId, EtlOutcome outcome) {
        String preview = outcome.fullText().length() > PREVIEW_MAX_CHARS
                ? outcome.fullText().substring(0, PREVIEW_MAX_CHARS) : outcome.fullText();
        if (documentMapper.completeProcessing(documentId, preview, outcome.chunkCount()) == 0) {
            // 执行中被删：不复活已删文档，清掉本次写入的残留产物
            chunkMapper.deleteByDocumentId(documentId);
        }
        log.info("ETL completed: documentId={}, chunks={}, vectors={}",
                documentId, outcome.chunkCount(), outcome.vectorCount());
    }

    /** 结算失败段：文档立即置 FAILED（不再卡 PROCESSING 等重启），原因截断防超列。 */
    private void settleFailure(Long documentId, Exception e) {
        log.error("ETL failed: documentId={}", documentId, e);
        String message = e.getMessage() == null ? "未知错误" : e.getMessage();
        documentMapper.markFailed(documentId, message.length() > REASON_MAX_CHARS
                ? message.substring(0, REASON_MAX_CHARS) : message);
    }

    /** ETL 段的产出：整篇清洗后文本（供预览截断）+ 切片数 + 成功向量化的切片数。 */
    private record EtlOutcome(String fullText, int chunkCount, int vectorCount) {
    }
}
