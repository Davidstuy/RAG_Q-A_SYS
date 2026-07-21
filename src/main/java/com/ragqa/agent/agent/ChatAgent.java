package com.ragqa.agent.agent;

import com.ragqa.agent.model.AgentResponse;
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
 * Chat Agent - 處理一般對話
 *
 * 職責：
 * - 問候語
 * - 簡單對話
 * - 不需要文檔的常識問題
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAgent {

    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;

    /**
     * 處理一般對話（非流式）
     */
    public AgentResponse chat(String userMessage, String context) {
        log.info("Chat Agent 處理: {}", userMessage);

        String prompt = buildPrompt(userMessage, context);

        try {
            List<ChatMessage> messages = List.of(
                SystemMessage.from("你是一個友善的智能助手，擅長用自然語言與用戶交流。回答要簡潔、友好。"),
                UserMessage.from(prompt)
            );

            AiMessage response = chatModel.generate(messages).content();
            return AgentResponse.success(response.text());

        } catch (Exception e) {
            log.error("Chat Agent 生成失敗", e);
            return AgentResponse.error("生成回應時出現錯誤");
        }
    }

    /**
     * 處理一般對話（流式）
     */
    public Flux<String> streamChat(String userMessage, String context) {
        log.info("Chat Agent 流式處理: {}", userMessage);

        String prompt = buildPrompt(userMessage, context);

        List<ChatMessage> messages = List.of(
            SystemMessage.from("你是一個友善的智能助手，擅長用自然語言與用戶交流。回答要簡潔、友好。"),
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
                    log.info("Chat Agent 流式完成");
                }

                @Override
                public void onError(Throwable error) {
                    log.error("Chat Agent 流式失敗", error);
                    sink.error(error);
                }
            });
        });
    }

    private String buildPrompt(String userMessage, String context) {
        StringBuilder sb = new StringBuilder();

        if (context != null && !context.isEmpty()) {
            sb.append("對話歷史:\n").append(context).append("\n\n");
        }

        sb.append("用戶說: ").append(userMessage).append("\n\n");
        sb.append("請用自然、友善的語言回應用戶。");

        return sb.toString();
    }
}
