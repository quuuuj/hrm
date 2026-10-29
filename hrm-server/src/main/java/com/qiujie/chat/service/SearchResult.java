package com.qiujie.chat.service;

import org.springframework.lang.Nullable;

/**
 * 知识库检索结果（向量/关键词/RRF 融合/邻窗扩展各阶段统一载体）。
 * <p>
 * {@code score} 是融合后的排序分（RRF 归一化到 [0,1]），只用于排序与展示；
 * {@code relevance} 保留检索通道的原始相关度（向量相似度 / 关键词命中分），
 * 供证据充分度评估使用——RRF 归一化会把首条结果强制拉成 1.0，拿它算充分度会恒判「充分」。
 * </p>
 * <p>
 * {@code channels} 记录该切片在各检索通道上的原始得分，供引用快照回填
 * {@code kb_qa_citation} 的 vector_score / keyword_score / hybrid_score。
 * 它不是评估依据（那些值在 {@code relevance} 里已经算过），只是审计用的旁路信息，
 * 因此整条链路允许它为 null。
 * </p>
 */
public record SearchResult(String chunkText, String documentName, Long documentId,
                           Long chunkId, double score, String source, double relevance,
                           @Nullable ChannelScores channels) {

    /**
     * 各检索通道的原始得分；该切片未命中某通道时对应字段为 null。
     * <p>{@code hybrid} 是 RRF 融合后的归一化得分（= 同阶段的 {@code score}）。</p>
     */
    public record ChannelScores(@Nullable Double vector, @Nullable Double keyword,
                                @Nullable Double hybrid) {

        /** 只命中向量通道。 */
        public static ChannelScores vector(double score) {
            return new ChannelScores(score, null, null);
        }

        /** 只命中关键词通道。 */
        public static ChannelScores keyword(double score) {
            return new ChannelScores(null, score, null);
        }

        /** 合并两路得分（任一可为 null），用于 RRF 融合阶段。 */
        public ChannelScores merge(@Nullable ChannelScores other) {
            if (other == null) {
                return this;
            }
            return new ChannelScores(
                    other.vector != null ? other.vector : this.vector,
                    other.keyword != null ? other.keyword : this.keyword,
                    other.hybrid != null ? other.hybrid : this.hybrid);
        }

        /** 补上融合得分。 */
        public ChannelScores withHybrid(double score) {
            return new ChannelScores(vector, keyword, score);
        }

        /** 检索来源：两路都有为 BOTH，否则取命中的那一路；两路都没有返回 null。 */
        @Nullable
        public String retrievalSource() {
            boolean hasVector = vector != null;
            boolean hasKeyword = keyword != null;
            if (hasVector && hasKeyword) {
                return "BOTH";
            }
            return hasVector ? "VECTOR" : hasKeyword ? "KEYWORD" : null;
        }
    }

    /** 原始相关度缺省等于排序分（关键词/邻窗等无独立相关度语义的场景）。 */
    public SearchResult(String chunkText, String documentName, Long documentId,
                        Long chunkId, double score, String source) {
        this(chunkText, documentName, documentId, chunkId, score, source, score, null);
    }

    /** 无通道得分的七参构造（历史调用点）。 */
    public SearchResult(String chunkText, String documentName, Long documentId,
                        Long chunkId, double score, String source, double relevance) {
        this(chunkText, documentName, documentId, chunkId, score, source, relevance, null);
    }

    /** 换一个排序分但保留通道得分（邻窗扩展、RRF 归一化等派生场景）。 */
    public SearchResult withScore(double newScore) {
        return new SearchResult(chunkText, documentName, documentId, chunkId,
                newScore, source, relevance, channels);
    }
}
