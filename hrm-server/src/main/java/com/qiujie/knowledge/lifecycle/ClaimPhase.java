package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.entity.KnowledgeIngestionTask;
import com.qiujie.knowledge.mapper.KnowledgeIngestionTaskMapper;
import com.qiujie.knowledge.mapper.DocsMapper;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认领阶段：文档加载 → CAS 认领 → 建任务。
 * <p>
 * CAS 是唯一的并发互斥原语：并发触发/恢复重发只有一个管道能通过（其余静默跳过）。
 * 未认领（文档不存在 / 状态不允许 / 已删除）返回 {@link Optional#empty()}，不建任务、不写状态。
 */
final class ClaimPhase {

    private final DocsMapper docsMapper;
    private final KnowledgeIngestionTaskMapper taskMapper;

    ClaimPhase(DocsMapper docsMapper, KnowledgeIngestionTaskMapper taskMapper) {
        this.docsMapper = docsMapper;
        this.taskMapper = taskMapper;
    }

    /** 认领产物：承载后续阶段与失败结算所需的全部上下文。 */
    record Claimed(Docs doc, KnowledgeIngestionTask task) {}

    Optional<Claimed> execute(Long docsId) {
        Docs doc = docsMapper.selectById(docsId);
        if (doc == null) {
            return Optional.empty();
        }
        // CAS 认领：并发触发/恢复重发只有一个管道能跑
        if (docsMapper.claimForProcessing(docsId) == 0) {
            return Optional.empty();
        }

        KnowledgeIngestionTask task = new KnowledgeIngestionTask()
                .setDocsId(docsId)
                .setStaffId(doc.getStaffId())
                .setTaskType("INGEST_DOCS")
                .setStatus("RUNNING")
                .setStartTime(LocalDateTime.now())
                .setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        return Optional.of(new Claimed(doc, task));
    }
}
