package com.qiujie.chat.service;

import com.qiujie.knowledge.lifecycle.VectorMetadata;
import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索服务：向量检索 + 关键词检索 → RRF 融合。
 * <p>
 * 遵循 RFC #69：关键词检索改从合并后的 {@code sys_docs} 表关联查询。
 * </p>
 */
@Service
public class HybridRetrievalService implements KnowledgeSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);

    @Autowired
    private DashScopeEmbeddingClient embeddingClient;

    private final JdbcTemplate kbJdbc;

    public HybridRetrievalService(@Qualifier("kbDataSource") DataSource kbDataSource) {
        this.kbJdbc = new JdbcTemplate(kbDataSource);
    }

    @Value("${knowledge.retrieval.top-k:10}")
    private int topK;

    @Value("${knowledge.retrieval.vector-top-k:20}")
    private int vectorTopK;

    @Value("${knowledge.retrieval.keyword-top-k:20}")
    private int keywordTopK;

    @Value("${knowledge.retrieval.rrf-k:60}")
    private int rrfK;

    @Value("${knowledge.retrieval.window-size:1}")
    private int windowSize;

    @Override
    public List<SearchResult> search(List<String> queries) {
        List<SearchResult> allResults = new ArrayList<>();
        for (String query : queries) {
            allResults.addAll(vectorSearch(query));
            allResults.addAll(keywordSearch(query));
        }
        if (allResults.isEmpty()) return List.of();
        List<SearchResult> fused = rrfFuse(allResults);
        fused.sort((a, b) -> Double.compare(b.score(), a.score()));
        if (fused.size() > topK) fused = fused.subList(0, topK);
        return expandNeighborWindows(fused);
    }

    private List<SearchResult> vectorSearch(String query) {
        try {
            float[] vec = embeddingClient.embed(List.of(query)).get(0);
            com.pgvector.PGvector pgVec = new com.pgvector.PGvector(vec);
            String sql = "SELECT content, metadata, 1 - (embedding <=> ?) AS similarity FROM vector_store ORDER BY embedding <=> ? LIMIT ?";
            return kbJdbc.query(sql, rs -> {
                List<SearchResult> list = new ArrayList<>();
                while (rs.next()) {
                    String metaStr = rs.getString("metadata");
                    double sim = rs.getDouble("similarity");
                    VectorMetadata meta = VectorMetadata.fromJson(metaStr);
                    list.add(new SearchResult(rs.getString("content"),
                            meta.documentName(), meta.documentId(), meta.chunkId(), sim, "vector"));
                }
                return list;
            }, pgVec, pgVec, vectorTopK);
        } catch (Exception e) {
            log.warn("Vector search failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SearchResult> keywordSearch(String query) {
        if (kbJdbc == null) return List.of();
        try {
            // RFC #69: kb_document 吸收并入 sys_docs (PG端存储为 document_chunk 关联的文档视图或仅 chunk 查询)
            // 在 PG 端若有 sys_docs 镜像或通过 chunk_text 直接查询
            String sql = """
                    SELECT c.chunk_text, COALESCE(d.old_name, ''), c.document_id, c.id
                    FROM document_chunk c
                    LEFT JOIN kb_document d ON d.id = c.document_id
                    WHERE c.chunk_text ILIKE ?
                    ORDER BY length(c.chunk_text) ASC
                    LIMIT ?
                    """;
            String pattern = "%" + query.replace("%", "\\%").replace("_", "\\_") + "%";
            return kbJdbc.query(sql, (rs, rowNum) -> {
                double score = 0.6;
                String text = rs.getString(1);
                int count = countMatches(text.toLowerCase(), query.toLowerCase());
                score += Math.min(count * 0.1, 0.3);
                return new SearchResult(text, rs.getString(2), rs.getLong(3), rs.getLong(4), score, "keyword");
            }, pattern, keywordTopK);
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<SearchResult> rrfFuse(List<SearchResult> results) {
        Map<String, List<SearchResult>> bySource = results.stream()
                .collect(Collectors.groupingBy(SearchResult::source));
        List<SearchResult> vectorRanked = bySource.getOrDefault("vector", List.of())
                .stream().sorted((a, b) -> Double.compare(b.score(), a.score())).toList();
        List<SearchResult> keywordRanked = bySource.getOrDefault("keyword", List.of())
                .stream().sorted((a, b) -> Double.compare(b.score(), a.score())).toList();
        Map<String, SearchResult> fused = new LinkedHashMap<>();
        int k = rrfK;
        for (int i = 0; i < vectorRanked.size(); i++) {
            SearchResult r = vectorRanked.get(i);
            String key = r.documentId() + ":" + r.chunkId();
            fused.put(key, new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), 1.0 / (k + i + 1), "rrf"));
        }
        for (int i = 0; i < keywordRanked.size(); i++) {
            SearchResult r = keywordRanked.get(i);
            String key = r.documentId() + ":" + r.chunkId();
            double rrf = 1.0 / (k + i + 1);
            if (fused.containsKey(key)) {
                SearchResult existing = fused.get(key);
                fused.put(key, new SearchResult(existing.chunkText(), existing.documentName(), existing.documentId(), existing.chunkId(), existing.score() + rrf, "rrf"));
            } else {
                fused.put(key, new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), rrf, "rrf"));
            }
        }
        double maxScore = fused.values().stream().mapToDouble(SearchResult::score).max().orElse(1.0);
        return fused.values().stream()
                .map(r -> new SearchResult(r.chunkText(), r.documentName(), r.documentId(), r.chunkId(), r.score() / maxScore, "rrf"))
                .sorted((a, b) -> Double.compare(b.score(), a.score())).collect(Collectors.toList());
    }

    private List<SearchResult> expandNeighborWindows(List<SearchResult> top) {
        if (kbJdbc == null || windowSize <= 0) return top;
        Set<String> existingKeys = top.stream().map(r -> r.documentId() + ":" + r.chunkId()).collect(Collectors.toSet());
        List<SearchResult> expanded = new ArrayList<>(top);
        for (SearchResult r : top) {
            if (r.chunkId() == null || r.documentId() == null) continue;
            try {
                String sql = """
                        SELECT c.chunk_text, COALESCE(d.old_name, ''), c.document_id, c.id, c.chunk_index
                        FROM document_chunk c
                        LEFT JOIN kb_document d ON d.id = c.document_id
                        WHERE c.document_id = ? AND c.chunk_index BETWEEN (SELECT chunk_index - ? FROM document_chunk WHERE id = ?)
                            AND (SELECT chunk_index + ? FROM document_chunk WHERE id = ?)
                        """;
                kbJdbc.query(sql, (rs, rowNum) -> {
                    String key = rs.getLong(3) + ":" + rs.getLong(4);
                    if (!existingKeys.contains(key)) {
                        existingKeys.add(key);
                        expanded.add(new SearchResult(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4), r.score() * 0.8, "neighbor"));
                    }
                    return null;
                }, r.documentId(), windowSize, r.chunkId(), windowSize, r.chunkId());
            } catch (Exception ignored) {}
        }
        return expanded;
    }

    private static int countMatches(String text, String query) {
        if (text == null || query == null) return 0;
        int count = 0, idx = 0;
        String lowerText = text.toLowerCase(), lowerQuery = query.toLowerCase();
        while ((idx = lowerText.indexOf(lowerQuery, idx)) != -1) { count++; idx += lowerQuery.length(); }
        return count;
    }
}
