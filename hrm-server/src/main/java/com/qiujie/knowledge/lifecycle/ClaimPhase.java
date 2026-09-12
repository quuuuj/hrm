package com.qiujie.knowledge.lifecycle;

import com.qiujie.knowledge.entity.Docs;
import com.qiujie.knowledge.entity.IngestionJob;
import com.qiujie.knowledge.mapper.IngestionJobMapper;
import com.qiujie.knowledge.mapper.DocsMapper;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 认领阶段：文档加载 → CAS 认领 → 建作业。
 * <p>
 * CAS 是唯一的并发互斥原语：并发触发/恢复重发只有一个管道能通过（其余静默跳过）。
 * 未认领（文档不存在 / 状态不允许 / 已删除）返回 {@link Optional#empty()}，不建作业、不写状态。
 */
final class ClaimPhase {

    private final DocsMapper documentMapper;
    private final IngestionJobMapper jobMapper;

    ClaimPhase(DocsMapper documentMapper, IngestionJobMapper jobMapper) {
        this.documentMapper = documentMapper;
        this.jobMapper = jobMapper;
    }

    /** 认领产物：承载后续阶段与失败结算所需的全部上下文。 */
    record Claimed(Docs doc, IngestionJob job) {}

    Optional<Claimed> execute(Long documentId) {
        Docs doc = documentMapper.selectById(documentId);
        if (doc == null) {
            return Optional.empty();
        }
        // CAS 认领：并发触发/恢复重发只有一个管道能跑
        if (documentMapper.claimForProcessing(documentId) == 0) {
            return Optional.empty();
        }

        IngestionJob job = new IngestionJob()
                .setDocumentId(documentId)
                .setStaffId(doc.getStaffId())
                .setJobType("INGEST_DOCUMENT")
                .setStatus("RUNNING")
                .setStartedAt(LocalDateTime.now())
                .setCreateTime(LocalDateTime.now());
        jobMapper.insert(job);
        return Optional.of(new Claimed(doc, job));
    }
}
