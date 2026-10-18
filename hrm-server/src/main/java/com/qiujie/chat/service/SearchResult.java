package com.qiujie.chat.service;

/**
 * 知识库检索结果（向量/关键词/RRF 融合/邻窗扩展各阶段统一载体）。
 */
public record SearchResult(String chunkText, String documentName, Long documentId,
                           Long chunkId, double score, String source) {}
