package com.ragqa.agent.agent;

import com.ragqa.agent.model.AgentResponse;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;
import com.ragqa.agent.tool.Tool;
import com.ragqa.agent.tool.ToolRegistry;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * RAG Agent - 處理需要企業資料的問題
 *
 * 職責：
 * - 文檔檢索
 * - 知識問答
 * - 需要內部資料的問題
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagAgent {

    private final ToolRegistry toolRegistry;
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;

    /**
     * RAG 相關性閾值
     */
    private static final double RELEVANCE_THRESHOLD = 0.6;

    /**
     * 處理 RAG 問題（非流式）
     */
    public AgentResponse chat(String userMessage, String context) {
        log.info("RAG Agent 處理: {}", userMessage);

        // 1. RAG 檢索
        Tool documentSearchTool = toolRegistry.getTool("document_search");
        ToolInput ragInput = new ToolInput();
        ragInput.setParameter("query", userMessage);

        ToolExecution ragResult = documentSearchTool.execute(ragInput);

        // 2. 判斷相關性
        if (ragResult.isSuccess() && ragResult.getMetadata() != null) {
            Object relevanceObj = ragResult.getMetadata().get("relevanceScore");
            if (relevanceObj instanceof Number) {
                double relevanceScore = ((Number) relevanceObj).doubleValue();
                log.info("RAG 相關性: {}", relevanceScore);

                // 3. 相關性高，使用 RAG 結果
                if (relevanceScore >= RELEVANCE_THRESHOLD) {
                    log.info("使用 RAG 結果（相關性 {} >= {}）", relevanceScore, RELEVANCE_THRESHOLD);
                    return AgentResponse.success(ragResult.getResult(),
                        "根據文檔找到相關信息：\n" + ragResult.getResultAsString());
                }
            }
        }

        // 4. 相關性低，結合 LLM 回答
        log.info("RAG 相關性不足，結合 LLM 回答");
        return handleHybridResponse(userMessage, context,
            ragResult.isSuccess() ? ragResult.getResultAsString() : "");
    }

    /**
     * 處理 RAG 問題（流式）
     */
    public Flux<String> streamChat(String userMessage, String context) {
        log.info("RAG Agent 流式處理: {}", userMessage);

        // 1. RAG 檢索
        Tool documentSearchTool = toolRegistry.getTool("document_search");
        ToolInput ragInput = new ToolInput();
        ragInput.setParameter("query", userMessage);

        ToolExecution ragResult = documentSearchTool.execute(ragInput);

        // 2. 判斷相關性
        String ragContext = "";
        if (ragResult.isSuccess() && ragResult.getMetadata() != null) {
            Object relevanceObj = ragResult.getMetadata().get("relevanceScore");
            if (relevanceObj instanceof Number) {
                double relevanceScore = ((Number) relevanceObj).doubleValue();
                log.info("RAG 相關性: {}", relevanceScore);

                // 3. 相關性高，直接返回 RAG 結果
                if (relevanceScore >= RELEVANCE_THRESHOLD) {
                    log.info("使用 RAG 結果（相關性 {} >= {}）", relevanceScore, RELEVANCE_THRESHOLD);
                    return Flux.just(ragResult.getResultAsString());
                }
            }
            ragContext = ragResult.getResultAsString();
        }

        // 4. 相關性低，流式結合 LLM 回答
        log.info("RAG 相關性不足，流式結合 LLM 回答");
        return streamHybridResponse(userMessage, context, ragContext);
    }

    /**
     * 混合回應：結合 RAG 結果和 LLM（非流式）
     */
    private AgentResponse handleHybridResponse(String userMessage, String context, String ragContext) {
        String prompt = buildHybridPrompt(userMessage, context, ragContext);

        try {
            List<ChatMessage> messages = List.of(
                SystemMessage.from("你是一個智能助手，善於結合文檔和知識回答問題。"),
                UserMessage.from(prompt)
            );

            AiMessage response = chatModel.generate(messages).content();
            return AgentResponse.success(response.text());

        } catch (Exception e) {
            log.error("混合回應生成失敗", e);
            return AgentResponse.success(ragContext); // 降級使用 RAG 結果
        }
    }

    /**
     * 混合回應：結合 RAG 結果和 LLM（流式）
     */
    private Flux<String> streamHybridResponse(String userMessage, String context, String ragContext) {
        String prompt = buildHybridPrompt(userMessage, context, ragContext);

        List<ChatMessage> messages = List.of(
            SystemMessage.from("你是一個智能助手，善於結合文檔和知識回答問題。"),
            UserMessage.from(prompt)
        );

        return Flux.create(sink -> {
            streamingChatModel.generate(messages, new StreamingResponseHandler<>() {
                @Override
                public void onNext(String token) {
                    sink.next(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    sink.complete();
                    log.info("RAG Agent 流式完成");
                }

                @Override
                public void onError(Throwable error) {
                    log.error("RAG Agent 流式失敗", error);
                    sink.error(error);
                }
            });
        });
    }

    private String buildHybridPrompt(String userMessage, String context, String ragContext) {
        return String.format("""
            你是一個智能助手。請結合以下文檔信息和你的知識來回答用戶問題。

            文檔信息：
            %s

            對話歷史：
            %s

            用戶問題：%s

            請注意：
            1. 優先使用文檔中的信息
            2. 如果文檔信息不足，可以補充你的知識
            3. 請明確標注信息來源
            """, ragContext, context != null ? context : "無", userMessage);
    }
}
