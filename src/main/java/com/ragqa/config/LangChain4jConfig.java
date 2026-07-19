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
import com.ragqa.store.QdrantRestEmbeddingStore;
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
     * - QdrantRestEmbeddingStore: 自定义实现，通过 REST API 访问 Qdrant
     *
     * 为什么用自定义实现而不是官方 langchain4j-qdrant？
     * - 官方 gRPC 客户端与 Qdrant 1.18 存在向量格式兼容性问题
     * - REST API 更简单、更稳定
     * - 完全兼容 EmbeddingStore 接口，其他代码无需修改
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new QdrantRestEmbeddingStore(qdrantHost, qdrantPort, qdrantCollection);
    }
}