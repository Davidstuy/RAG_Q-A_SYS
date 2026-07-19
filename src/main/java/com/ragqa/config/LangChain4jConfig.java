package com.ragqa.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.model.chat}")
    private String chatModelName;
    

    @Value("${openai.model.embedding}")
    private String embeddingModelName;

    @Value("${qdrant.host}")
    private String qdrantHost;

    @Value("${qdrant.port}")
    private int qdrantPort;

    @Value("${qdrant.collection}")
    private String qdrantCollection;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(chatModelName)
            .temperature(0.7)
            .build();
    }

    /**
     * 配置流式聊天模型（逐 token 返回）
     * 教学：StreamingChatLanguageModel 与 ChatLanguageModel 的区别：
     * - ChatLanguageModel: 同步调用，等 LLM 生成完整回答后一次性返回
     * - StreamingChatLanguageModel: 异步调用，每生成一个 token 就回调一次
     */
    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .modelName(chatModelName)
            .temperature(0.7)
            .build();
    }

    /**
     * 配置 Embedding 模型
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * 配置向量存储 —— Qdrant 持久化向量数据库
     *
     * 教学：EmbeddingStore 是 LangChain4j 的向量存储接口
     * - InMemoryEmbeddingStore: 数据存在内存，重启丢失（之前用的）
     * - QdrantEmbeddingStore: 数据存在 Qdrant，重启不丢（现在用的）
     *
     * 由于 ChatService 和 DocumentService 都依赖接口而非实现，
     * 切换存储后其他代码完全不用改 —— 这就是面向接口编程的好处
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return QdrantEmbeddingStore.builder()
            .host(qdrantHost)
            .port(qdrantPort)
            .collectionName(qdrantCollection)
            .build();
    }
}