package com.qiujie.document.lifecycle;

import com.qiujie.document.lifecycle.port.ChunkVectorStore;
import com.qiujie.document.mapper.DocumentMapper;

/**
 * 结算阶段：PROCESSING → READY / CANCELLED / FAILED 的状态机收口。
 * <p>
 * PG 侧产物已写入，此处只负责文档状态与失败补偿——
 * 执行中被删（completeProcessing 落空）时清理本次残留产物，不复活已删文档。
 */
final class SettlePhase {

    private final DocumentMapper documentMapper;
    private final ChunkVectorStore chunkVectorStore;

    SettlePhase(DocumentMapper documentMapper, ChunkVectorStore chunkVectorStore) {
        this.documentMapper = documentMapper;
        this.chunkVectorStore = chunkVectorStore;
    }

    /** 成功结算：PROCESSING → READY + 预览/切片数；执行中被删则清理残留。 */
    void succeed(ClaimPhase.Claimed claimed, EtlPhase.Extracted extracted) {
        var document = claimed.document();

        String preview = preview(extracted.fullText());
        if (documentMapper.completeProcessing(document.getId(), preview, extracted.chunkCount()) == 0) {
            // 执行中被删：不复活已删文档，清掉本次写入的残留产物
            chunkVectorStore.deleteChunks(document.getId());
        }
    }

    /** 失败补偿：文档立即置 FAILED（不再卡 PROCESSING 等重启）。 */
    void fail(ClaimPhase.Claimed claimed, Exception e) {
        documentMapper.markFailed(claimed.document().getId(), truncate(e.getMessage(), 500));
    }

    private static String preview(String fullText) {
        return fullText.length() > 500 ? fullText.substring(0, 500) : fullText;
    }

    /** 截断字符串，防止错误信息超出数据库字段长度 */
    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "未知错误";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
