package com.qiujie.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DashScope 文本重排序客户端（原生 API）。
 * <p>
 * 重排序是检索期能力，只有问答链路使用，因此放在 chat 包内（知识库摄入不依赖它）；
 * 配置沿用 {@code knowledge.retrieval.*} 前缀，与 top-k / rrf-k 等检索参数同处一节。
 * </p>
 * <p>
 * 失败语义：任何异常都降级为「原样返回候选」，检索链路不因重排不可用而中断——
 * RRF 融合结果本身就是可用的排序，重排只是精排。
 * </p>
 */
@Service
public class DashScopeRerankClient {

    private static final Logger log = LoggerFactory.getLogger(DashScopeRerankClient.class);

    @Value("${knowledge.retrieval.rerank.enabled}")
    private boolean enabled;

    @Value("${knowledge.retrieval.rerank.model}")
    private String model;

    @Value("${knowledge.embedding.api-key}")
    private String apiKey;

    @Value("${knowledge.embedding.base-url}")
    private String baseUrl;

    private RestTemplate restTemplate;
    private String rerankUrl;
    /** 纯对象，无外部依赖，直接初始化（便于解析逻辑单测）。 */
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
        // 与嵌入客户端一致：从兼容模式 baseUrl 推导原生 API 地址
        String host = baseUrl == null ? "" : baseUrl.replace("/compatible-mode", "");
        this.rerankUrl = host + "/api/v1/services/rerank/text-rerank/text-rerank";
        log.info("DashScope rerank: enabled={}, model={}, url={}", enabled, model, rerankUrl);
    }

    /**
     * 对候选做精排，返回按重排分数降序的结果；候选按 chunkId 去重（取最高分）。
     *
     * @param query      检索问题（重排模型需要单一查询语义，调用方传改写后的主查询）
     * @param candidates RRF 融合后的候选
     * @param topN       保留条数
     * @return 精排结果；未启用或无候选项时原样返回候选
     */
    public List<SearchResult> rerank(String query, List<SearchResult> candidates, int topN) {
        if (!enabled || candidates == null || candidates.isEmpty() || query == null || query.isBlank()) {
            return candidates;
        }
        List<String> documents = candidates.stream().map(SearchResult::chunkText).toList();
        List<Double> scores = requestScores(query, documents);
        if (scores == null) {
            return candidates;
        }

        List<SearchResult> reranked = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            SearchResult c = candidates.get(i);
            double score = scores.get(i);
            // score 决定排序，relevance 供证据充分度评估使用——重排分是真正的语义相关度
            // channels 原样透传：重排不改变「这条从哪个检索通道来的」这一事实
            reranked.add(new SearchResult(c.chunkText(), c.documentName(), c.documentId(),
                    c.chunkId(), score, "rerank", score, c.channels()));
        }
        reranked.sort((a, b) -> Double.compare(b.score(), a.score()));
        log.info("Reranked {} candidates, top score={}", reranked.size(),
                reranked.isEmpty() ? 0 : String.format("%.3f", reranked.get(0).score()));
        return reranked.size() > topN ? new ArrayList<>(reranked.subList(0, topN)) : reranked;
    }

    /** 调重排接口拿逐条分数；失败返回 null（调用方原样返回候选）。 */
    private List<Double> requestScores(String query, List<String> documents) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);
            input.put("documents", documents);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("return_documents", false);
            parameters.put("top_n", documents.size());

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("input", input);
            body.put("parameters", parameters);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String response = restTemplate.postForObject(rerankUrl, new HttpEntity<>(body, headers), String.class);
            return parseScores(response, documents.size());
        } catch (Exception e) {
            log.warn("DashScope rerank failed, fallback to RRF order: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析响应，返回与 documents 等长的分数表。
     * <p>
     * 响应形如 {@code {"output":{"results":[{"index":0,"relevance_score":0.89}]}}}；
     * 缺失的 index 记 0 分（保持位置对齐，避免错位打分）。
     * </p>
     */
    List<Double> parseScores(String response, int expectedSize) {
        if (response == null || response.isBlank()) {
            return null;
        }
        try {
            JsonNode results = mapper.readTree(response).path("output").path("results");
            if (!results.isArray() || results.size() == 0) {
                return null;
            }
            List<Double> scores = new ArrayList<>();
            for (int i = 0; i < expectedSize; i++) {
                scores.add(0.0);
            }
            for (JsonNode item : results) {
                int index = item.path("index").asInt(-1);
                if (index < 0 || index >= expectedSize) {
                    continue;
                }
                scores.set(index, item.path("relevance_score").asDouble(0));
            }
            return scores;
        } catch (Exception e) {
            log.warn("DashScope rerank response parse failed: {}", e.getMessage());
            return null;
        }
    }
}
