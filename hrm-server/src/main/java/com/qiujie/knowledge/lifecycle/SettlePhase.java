package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.DocsMapper;

import java.time.LocalDateTime;

/**
 * 结算阶段：PROCESSING → READY / CANCELLED / FAILED 的状态机收口。
 * <p>
 * PG 侧产物已写入，此处只负责文档状态、作业状态与失败补偿——
 * 执行中被删（completeProcessing 落空）时清理本次残留产物，不复活已删文档。
 */
final class SettlePhase {

    private final DocsMapper documentMapper;
    private final IngestionJobMapper jobMapper;
    private final ChunkVectorStore chunkVectorStore;

    SettlePhase(DocsMapper documentMapper, IngestionJobMapper jobMapper, ChunkVectorStore chunkVectorStore) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
        this.chunkVectorStore = chunkVectorStore;
    }

    /** 成功结算：PROCESSING → READY + 预览/切片数；执行中被删则取消作业并清理残留。 */
    void succeed(ClaimPhase.Claimed claimed, EtlPhase.Extracted extracted) {
        var doc = claimed.doc();
        var job = claimed.job();

        String preview = preview(extracted.fullText());
        if (documentMapper.completeProcessing(doc.getId().longValue(), preview, extracted.chunkCount()) == 0) {
            // 执行中被删：不复活已删文档，清掉本次写入的残留产物
            chunkVectorStore.deleteChunks(doc.getId().longValue());
            chunkVectorStore.deleteDocumentMirror(doc.getId().longValue());
            finish(job, "CANCELLED", null);
        } else {
            finish(job, "SUCCEEDED", null);
        }
    }

    /** 失败补偿：作业 FAILED + 原因，文档立即置 FAILED（不再卡 PROCESSING 等重启）。 */
    void fail(ClaimPhase.Claimed claimed, Exception e) {
        String reason = truncate(e.getMessage(), 1000);
        finish(claimed.job(), "FAILED", reason);
        documentMapper.markFailed(claimed.doc().getId().longValue(), reason);
    }

    private void finish(IngestionJob job, String status, String error) {
        job.setStatus(status);
        if (error != null) {
            job.setLastError(error);
        }
        job.setFinishedAt(LocalDateTime.now());
        jobMapper.updateById(job);
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
