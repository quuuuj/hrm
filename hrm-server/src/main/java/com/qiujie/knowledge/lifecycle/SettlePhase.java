package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.KnowledgeIngestionTask;
import com.qiujie.knowledge.lifecycle.port.ChunkVectorStore;
import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;

import java.time.LocalDateTime;

/**
 * 结算阶段：PROCESSING → READY / CANCELLED / FAILED 的状态机收口。
 * <p>
 * PG 侧产物已写入，此处只负责文档状态、任务状态与失败补偿——
 * 执行中被删（completeProcessing 落空）时清理本次残留产物，不复活已删文档。
 */
final class SettlePhase {

    private final DocsMapper docsMapper;
    private final KnowledgeIngestionTaskMapper taskMapper;
    private final ChunkVectorStore chunkVectorStore;

    SettlePhase(DocsMapper docsMapper, KnowledgeIngestionTaskMapper taskMapper, ChunkVectorStore chunkVectorStore) {
        this.docsMapper = docsMapper;
        this.taskMapper = taskMapper;
        this.chunkVectorStore = chunkVectorStore;
    }

    /** 成功结算：PROCESSING → READY + 预览/切片数；执行中被删则取消任务并清理残留。 */
    void succeed(ClaimPhase.Claimed claimed, EtlPhase.Extracted extracted) {
        var doc = claimed.doc();
        var task = claimed.task();

        String preview = preview(extracted.fullText());
        if (docsMapper.completeProcessing(doc.getId().longValue(), preview, extracted.chunkCount()) == 0) {
            // 执行中被删：不复活已删文档，清掉本次写入的残留产物
            chunkVectorStore.deleteChunks(doc.getId().longValue());
            finish(task, "CANCELLED", null);
        } else {
            finish(task, "SUCCEEDED", null);
        }
    }

    /** 失败补偿：任务 FAILED + 原因，文档立即置 FAILED（不再卡 PROCESSING 等重启）。 */
    void fail(ClaimPhase.Claimed claimed, Exception e) {
        String reason = truncate(e.getMessage(), 1000);
        finish(claimed.task(), "FAILED", reason);
        docsMapper.markFailed(claimed.doc().getId().longValue(), reason);
    }

    private void finish(KnowledgeIngestionTask task, String status, String error) {
        task.setStatus(status);
        if (error != null) {
            task.setLastError(error);
        }
        task.setFinishTime(LocalDateTime.now());
        taskMapper.updateById(task);
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
