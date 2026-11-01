package com.qiujie.chat.service;

import com.qiujie.knowledge.service.DashScopeEmbeddingClient;
import jakarta.annotation.PostConstruct;
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
 * 混合检索服务：向量检索 + 关键词检索 → RRF 融合 → 重排序精排。
 * <p>
 * 两路检索都基于单表 {@code kb_chunk}（切片原文、文档名快照、向量同表）：
 * 向量通道走 HNSW 部分索引，关键词通道走 ILIKE；融合键为 chunk id，不需要跨表 JOIN。
 * RRF 只看排名不看语义相关度，精排由 DashScope rerank 模型补足；重排不可用时
 * 自动退回 RRF 顺序，检索链路不中断。
 * </p>
 */
@Service
public class HybridRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(HybridRetrievalService.class);

    @Autowired
    private DashScopeEmbeddingClient embeddingClient;

    @Autowired
    private DashScopeRerankClient rerankClient;

    @Autowired
    @Qualifier("kbDataSource")
    private DataSource kbDataSource;

    private JdbcTemplate kbJdbc;

    @PostConstruct
    public void init() {
        if (this.kbJdbc == null && this.kbDataSource != null) {
            this.kbJdbc = new JdbcTemplate(kbDataSource);
        }
    }

    @Value("${chat.retrieval.top-k}")
    private int topK;

    @Value("${chat.retrieval.vector-top-k}")
    private int vectorTopK;

    @Value("${chat.retrieval.keyword-top-k}")
    private int keywordTopK;

    @Value("${chat.retrieval.rrf-k}")
    private int rrfK;

    @Value("${chat.retrieval.window-size}")
    private int windowSize;

    @Value("${chat.retrieval.rerank.candidate-pool-size}")
    private int rerankPoolSize;

    public List<SearchResult> search(List<String> queries) {
        List<SearchResult> allResults = new ArrayList<>();
        for (String query : queries) {
            allResults.addAll(vectorSearch(query));
            allResults.addAll(keywordSearch(query));
        }
        if (allResults.isEmpty()) return List.of();
        List<SearchResult> fused = rrfFuse(allResults);
        fused.sort((a, b) -> Double.compare(b.score(), a.score()));

        // 先精排再截断：RRF 只看名次，topK 以内的语义顺序由重排模型决定
        List<SearchResult> pool = fused.size() > rerankPoolSize ? fused.subList(0, rerankPoolSize) : fused;
        String primaryQuery = queries.isEmpty() ? null : queries.get(0);
        List<SearchResult> ranked = rerankClient.rerank(primaryQuery, pool, topK);
        if (ranked.size() > topK) ranked = new ArrayList<>(ranked.subList(0, topK));
        return expandNeighborWindows(ranked);
    }

    private List<SearchResult> vectorSearch(String query) {
        try {
            float[] vec = embeddingClient.embed(List.of(query)).get(0);
            com.pgvector.PGvector pgVec = new com.pgvector.PGvector(vec);
            // 切片与向量同表：一次查询直接拿到文本与展示名，无需回表或解析 JSON
            String sql = """
                    SELECT chunk_text, docs_name, docs_id, id,
                           1 - (embedding <=> ?) AS similarity
                    FROM kb_chunk
                    WHERE embedding IS NOT NULL
                    ORDER BY embedding <=> ?
                    LIMIT ?
                    """;
            return kbJdbc.query(sql, rs -> {
                List<SearchResult> list = new ArrayList<>();
                while (rs.next()) {
                    double sim = rs.getDouble("similarity");
                    list.add(new SearchResult(rs.getString("chunk_text"),
                            rs.getString("docs_name"), rs.getLong("docs_id"),
                            rs.getLong("id"), sim, "vector", sim,
                            SearchResult.ChannelScores.vector(sim)));
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
            String sql = """
                    SELECT c.chunk_text, c.docs_name, c.docs_id, c.id
                    FROM kb_chunk c
                    WHERE c.chunk_text ILIKE ? ESCAPE '\\'
                    ORDER BY length(c.chunk_text) ASC
                    LIMIT ?
                    """;
            String pattern = "%" + query.replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_") + "%";
            return kbJdbc.query(sql, (rs, rowNum) -> {
                String text = rs.getString(1);
                int count = countMatches(text.toLowerCase(), query.toLowerCase());
                double relevance = 0.6 + Math.min(count * 0.1, 0.3);
                return new SearchResult(text, rs.getString(2), rs.getLong(3), rs.getLong(4),
                        relevance, "keyword", relevance, SearchResult.ChannelScores.keyword(relevance));
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
                .stream().sorted((a, b) -> Double.compare(b.relevance(), a.relevance())).toList();
        List<SearchResult> keywordRanked = bySource.getOrDefault("keyword", List.of())
                .stream().sorted((a, b) -> Double.compare(b.relevance(), a.relevance())).toList();
        Map<String, SearchResult> fused = new LinkedHashMap<>();
        int k = rrfK;
        for (int i = 0; i < vectorRanked.size(); i++) {
            SearchResult r = vectorRanked.get(i);
            String key = String.valueOf(r.chunkId());
            fused.put(key, new SearchResult(r.chunkText(), r.docsName(), r.docsId(), r.chunkId(),
                    1.0 / (k + i + 1), "rrf", r.relevance(), r.channels()));
        }
        for (int i = 0; i < keywordRanked.size(); i++) {
            SearchResult r = keywordRanked.get(i);
            String key = String.valueOf(r.chunkId());
            double rrf = 1.0 / (k + i + 1);
            if (fused.containsKey(key)) {
                SearchResult existing = fused.get(key);
                // 两路都命中的切片：保留两边原始分，供引用快照标注 BOTH
                fused.put(key, new SearchResult(existing.chunkText(), existing.docsName(),
                        existing.docsId(), existing.chunkId(), existing.score() + rrf, "rrf",
                        Math.max(existing.relevance(), r.relevance()),
                        existing.channels() == null ? r.channels() : existing.channels().merge(r.channels())));
            } else {
                fused.put(key, new SearchResult(r.chunkText(), r.docsName(), r.docsId(), r.chunkId(),
                        rrf, "rrf", r.relevance(), r.channels()));
            }
        }
        // score 归一化到 [0,1] 仅用于排序/展示；relevance 保持通道原始相关度供证据评估使用
        double maxScore = fused.values().stream().mapToDouble(SearchResult::score).max().orElse(1.0);
        return fused.values().stream()
                .map(r -> r.channels() == null
                        ? new SearchResult(r.chunkText(), r.docsName(), r.docsId(), r.chunkId(),
                                r.score() / maxScore, "rrf", r.relevance())
                        : new SearchResult(r.chunkText(), r.docsName(), r.docsId(), r.chunkId(),
                                r.score() / maxScore, "rrf", r.relevance(),
                                r.channels().withHybrid(r.score() / maxScore)))
                .sorted((a, b) -> Double.compare(b.score(), a.score())).collect(Collectors.toList());
    }

    private List<SearchResult> expandNeighborWindows(List<SearchResult> top) {
        if (kbJdbc == null || windowSize <= 0) return top;
        Set<String> existingKeys = top.stream().map(r -> r.docsId() + ":" + r.chunkId()).collect(Collectors.toSet());
        List<SearchResult> expanded = new ArrayList<>(top);
        for (SearchResult r : top) {
            if (r.chunkId() == null || r.docsId() == null) continue;
            try {
                String sql = """
                        SELECT c.chunk_text, c.docs_name, c.docs_id, c.id, c.chunk_index
                        FROM kb_chunk c
                        WHERE c.docs_id = ? AND c.chunk_index BETWEEN (SELECT chunk_index - ? FROM kb_chunk WHERE id = ?)
                            AND (SELECT chunk_index + ? FROM kb_chunk WHERE id = ?)
                        """;
                kbJdbc.query(sql, (rs, rowNum) -> {
                    String key = rs.getLong(3) + ":" + rs.getLong(4);
                    if (!existingKeys.contains(key)) {
                        existingKeys.add(key);
                        expanded.add(new SearchResult(rs.getString(1), rs.getString(2), rs.getLong(3), rs.getLong(4),
                                r.score() * 0.8, "neighbor", r.relevance(), r.channels()));
                    }
                    return null;
                }, r.docsId(), windowSize, r.chunkId(), windowSize, r.chunkId());
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
