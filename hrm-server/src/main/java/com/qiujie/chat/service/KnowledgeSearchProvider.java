package com.qiujie.chat.service;

import java.util.List;

/**
 * 知识库检索抽象。
 */
public interface KnowledgeSearchProvider {

    record SearchResult(String chunkText, String documentName, Long documentId,
                         Long chunkId, double score, String source) {}

    List<SearchResult> search(List<String> queries);
}
