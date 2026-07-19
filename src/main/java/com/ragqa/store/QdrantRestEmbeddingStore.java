package com.ragqa.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * 基于 Qdrant REST API 的向量存储实现
 *
 * 教学：为什么不用官方的 langchain4j-qdrant？
 * - 官方 gRPC 客户端与 Qdrant 1.18 存在向量格式兼容性问题
 * - REST API 更简单、更稳定，适合学习和中小规模项目
 *
 * 这个实现完全兼容 EmbeddingStore 接口，ChatService 和 DocumentService 无需修改
 */
@Slf4j
public class QdrantRestEmbeddingStore implements EmbeddingStore<TextSegment> {

    private final String baseUrl;
    private final String collectionName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public QdrantRestEmbeddingStore(String host, int port, String collectionName) {
        this.baseUrl = "http://" + host + ":" + port;
        this.collectionName = collectionName;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        // 确保 collection 存在
        ensureCollection();
    }

    /**
     * 确保 collection 存在，不存在则自动创建
     */
    private void ensureCollection() {
        try {
            HttpRequest checkRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collectionName))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(checkRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404 || response.body().contains("\"status\":\"error\"")) {
                String createBody = """
                    {
                        "vectors": {
                            "size": 1024,
                            "distance": "Cosine"
                        }
                    }
                    """;

                HttpRequest createRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/collections/" + collectionName))
                    .PUT(HttpRequest.BodyPublishers.ofString(createBody))
                    .header("Content-Type", "application/json")
                    .build();

                httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
                log.info("创建 Qdrant collection: {}", collectionName);
            } else {
                log.info("Qdrant collection 已存在: {}", collectionName);
            }
        } catch (Exception e) {
            log.error("检查/创建 Qdrant collection 失败", e);
            throw new RuntimeException("无法连接 Qdrant: " + e.getMessage(), e);
        }
    }

    @Override
    public String add(Embedding embedding) {
        return add(embedding, null);
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        return addAll(embeddings, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            ids.add(id);
            TextSegment segment = (embedded != null && i < embedded.size()) ? embedded.get(i) : null;
            addInternal(id, embeddings.get(i), segment);
        }
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        try {
            ObjectNode searchBody = objectMapper.createObjectNode();
            ArrayNode vectorArray = searchBody.putArray("vector");
            for (float v : referenceEmbedding.vector()) {
                vectorArray.add(v);
            }
            searchBody.put("limit", maxResults);
            searchBody.put("with_payload", true);
            searchBody.put("with_vector", true);
            if (minScore > 0) {
                searchBody.put("score_threshold", minScore);
            }

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points/search"))
                .POST(HttpRequest.BodyPublishers.ofString(searchBody.toString()))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Qdrant 搜索失败: {}", response.body());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode result = root.get("result");

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            if (result != null && result.isArray()) {
                for (JsonNode point : result) {
                    double score = point.get("score").asDouble();
                    JsonNode payload = point.get("payload");

                    String text = "";
                    if (payload != null && payload.has("text")) {
                        text = payload.get("text").asText();
                    }

                    JsonNode vectorNode = point.get("vector");
                    float[] vector = new float[0];
                    if (vectorNode != null && vectorNode.isArray()) {
                        vector = new float[vectorNode.size()];
                        for (int i = 0; i < vectorNode.size(); i++) {
                            vector[i] = (float) vectorNode.get(i).asDouble();
                        }
                    }

                    TextSegment segment = text.isEmpty() ? null : TextSegment.from(text);
                    Embedding emb = Embedding.from(vector);

                    matches.add(new EmbeddingMatch<>(
                        RelevanceScore.fromCosineSimilarity(score),
                        point.get("id").asText(),
                        emb,
                        segment
                    ));
                }
            }

            return matches;

        } catch (Exception e) {
            log.error("Qdrant 搜索异常", e);
            return Collections.emptyList();
        }
    }

    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        try {
            ObjectNode point = objectMapper.createObjectNode();
            point.put("id", id);

            ArrayNode vectorArray = point.putArray("vector");
            for (float v : embedding.vector()) {
                vectorArray.add(v);
            }

            if (textSegment != null) {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("text", textSegment.text());
                point.set("payload", payload);
            }

            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode points = requestBody.putArray("points");
            points.add(point);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collectionName + "/points"))
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Qdrant 添加向量失败: {}", response.body());
            }

        } catch (Exception e) {
            log.error("Qdrant 添加向量异常", e);
        }
    }
}
