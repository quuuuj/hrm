package com.qiujie.document.lifecycle;

import com.qiujie.document.entity.Document;
import com.qiujie.document.mapper.DocumentMapper;

import java.util.Optional;

/**
 * 认领阶段：文档加载 → CAS 认领。
 * <p>
 * CAS 是唯一的并发互斥原语：并发触发/恢复重发只有一个管道能通过（其余静默跳过）。
 * 未认领（文档不存在 / 状态不允许）返回 {@link Optional#empty()}，不写状态。
 */
final class ClaimPhase {

    private final DocumentMapper documentMapper;

    ClaimPhase(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /** 认领产物：承载后续阶段与失败结算所需的全部上下文。 */
    record Claimed(Document document) {}

    Optional<Claimed> execute(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            return Optional.empty();
        }
        // CAS 认领：并发触发/恢复重发只有一个管道能跑
        if (documentMapper.claimForProcessing(documentId) == 0) {
            return Optional.empty();
        }
        return Optional.of(new Claimed(document));
    }
}
