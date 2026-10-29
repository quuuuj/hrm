package com.qiujie.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 证据充分度评估服务。
 * 基于检索结果的数量、分数分布、文档多样性，判定证据等级。
 * <p>
 * 打分使用 {@link SearchResult#relevance()}（通道原始相关度），而不是 RRF 归一化后的
 * {@code score}：后者会把本批首条结果强制拉成 1.0，拿它评估会让单条结果也判为「充分」。
 * </p>
 */
@Service
public class EvidenceAssessmentService {

    @Value("${knowledge.qa.evidence-weak-threshold}")
    private double weakThreshold;

    @Value("${knowledge.qa.evidence-partial-threshold}")
    private double partialThreshold;

    @Value("${knowledge.qa.evidence-sufficient-threshold}")
    private double sufficientThreshold;

    public record Assessment(EvidenceLevel level, String reason) {}

    /**
     * 根据检索结果评估证据充分度。
     */
    public Assessment assess(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return new Assessment(EvidenceLevel.NONE, "知识库中未找到相关信息");
        }

        double maxScore = results.stream().mapToDouble(SearchResult::relevance).max().orElse(0);
        double avgScore = results.stream().mapToDouble(SearchResult::relevance).average().orElse(0);
        long uniqueDocs = results.stream().map(SearchResult::documentId).distinct().count();
        int count = results.size();

        // 多因素综合判定
        double compositeScore = maxScore * 0.4 + avgScore * 0.3
                + Math.min(uniqueDocs / 3.0, 1.0) * 0.15
                + Math.min(count / 5.0, 1.0) * 0.15;

        if (compositeScore >= sufficientThreshold) {
            return new Assessment(EvidenceLevel.SUFFICIENT,
                    String.format("找到 %d 条结果（%d 个文档），充分度 %.0f%%", count, uniqueDocs, compositeScore * 100));
        }
        if (compositeScore >= partialThreshold) {
            return new Assessment(EvidenceLevel.PARTIAL,
                    String.format("找到 %d 条部分相关结果，可能不够完整", count));
        }
        if (compositeScore >= weakThreshold) {
            return new Assessment(EvidenceLevel.WEAK,
                    String.format("仅找到 %d 条弱相关结果，信息可能不准确", count));
        }
        return new Assessment(EvidenceLevel.WEAK, "检索结果相关性较弱");
    }
}
