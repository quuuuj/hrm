package com.qiujie.chat.service;

/**
 * 知识库检索结果（向量/关键词/RRF 融合/邻窗扩展各阶段统一载体）。
 * <p>
 * {@code score} 是融合后的排序分（RRF 归一化到 [0,1]），只用于排序与展示；
 * {@code relevance} 保留检索通道的原始相关度（向量相似度 / 关键词命中分），
 * 供证据充分度评估使用——RRF 归一化会把首条结果强制拉成 1.0，拿它算充分度会恒判「充分」。
 * </p>
 */
public record SearchResult(String chunkText, String documentName, Long documentId,
                           Long chunkId, double score, String source, double relevance) {

    /** 原始相关度缺省等于排序分（关键词/邻窗等无独立相关度语义的场景）。 */
    public SearchResult(String chunkText, String documentName, Long documentId,
                        Long chunkId, double score, String source) {
        this(chunkText, documentName, documentId, chunkId, score, source, score);
    }
}
