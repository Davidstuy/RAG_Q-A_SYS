package com.ragqa.agent.service;

import com.ragqa.agent.agent.ChatAgent;
import com.ragqa.agent.agent.RagAgent;
import com.ragqa.agent.agent.ToolAgent;
import com.ragqa.agent.model.*;
import com.ragqa.agent.router.AgentRouter;
import com.ragqa.agent.router.AgentRouter.RouteResult;
import com.ragqa.agent.router.AgentRouter.RouteType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 服務 - 使用 Router 架構
 *
 * 流程：
 * 1. Router 判斷路由（規則匹配，0秒）
 * 2. Tool Agent / RAG Agent / Chat Agent 處理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRouter agentRouter;
    private final ChatAgent chatAgent;
    private final RagAgent ragAgent;
    private final ToolAgent toolAgent;
    private final DialogueManager dialogueManager;

    /**
     * 處理用戶請求（非流式）
     */
    public AgentResponse chat(AgentRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();

        log.info("處理 Agent 請求: sessionId={}, message={}", sessionId, userMessage);

        try {
            // 1. 獲取對話上下文
            String context = dialogueManager.getContextText(sessionId);

            // 2. Router 路由決策（規則匹配，無 LLM）
            RouteResult route = agentRouter.route(userMessage);
            log.info("路由決策: {}", route.getType());

            // 3. 根據路由結果分發到對應 Agent
            AgentResponse response;
            switch (route.getType()) {
                case TOOL:
                    response = toolAgent.chat(userMessage, route.getToolType());
                    break;
                case RAG_CHECK:
                    // 需要 RAG 檢索判斷
                    response = ragAgent.chat(userMessage, context);
                    break;
                case RAG:
                    response = ragAgent.chat(userMessage, context);
                    break;
                case CHAT:
                default:
                    response = chatAgent.chat(userMessage, context);
                    break;
            }

            // 4. 更新對話上下文
            IntentResult intent = new IntentResult(
                route.getToolType() != null ? route.getToolType() : IntentType.GENERAL_CHAT,
                1.0, new HashMap<>(), route.getType().name()
            );
            dialogueManager.updateContext(sessionId, userMessage, response.getMessage(), intent);

            return response;

        } catch (Exception e) {
            log.error("Agent 處理失敗", e);
            return AgentResponse.error("處理請求時出現錯誤: " + e.getMessage());
        }
    }

    /**
     * 處理用戶請求（流式）
     */
    public Flux<String> streamChat(AgentRequest request) {
        String sessionId = request.getSessionId();
        String userMessage = request.getMessage();

        log.info("處理流式 Agent 請求: sessionId={}, message={}", sessionId, userMessage);

        try {
            // 1. 獲取對話上下文
            String context = dialogueManager.getContextText(sessionId);

            // 2. Router 路由決策（規則匹配，無 LLM）
            RouteResult route = agentRouter.route(userMessage);
            log.info("路由決策: {}", route.getType());

            // 3. 根據路由結果分發到對應 Agent（流式）
            Flux<String> responseStream;
            switch (route.getType()) {
                case TOOL:
                    // Tool Agent 不支持流式，返回完整結果
                    AgentResponse toolResponse = toolAgent.chat(userMessage, route.getToolType());
                    responseStream = Flux.just(toolResponse.getData() != null ?
                        toolResponse.getData().toString() : toolResponse.getMessage());
                    break;
                case RAG_CHECK:
                    responseStream = ragAgent.streamChat(userMessage, context);
                    break;
                case RAG:
                    responseStream = ragAgent.streamChat(userMessage, context);
                    break;
                case CHAT:
                default:
                    responseStream = chatAgent.streamChat(userMessage, context);
                    break;
            }

            // 4. 更新對話上下文（在流完成後）
            IntentResult intent = new IntentResult(
                route.getToolType() != null ? route.getToolType() : IntentType.GENERAL_CHAT,
                1.0, new HashMap<>(), route.getType().name()
            );

            // 使用 doOnComplete 在流完成後更新上下文
            StringBuilder fullResponse = new StringBuilder();
            return responseStream.doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    dialogueManager.updateContext(sessionId, userMessage, fullResponse.toString(), intent);
                    log.info("流式完成，更新對話上下文");
                });

        } catch (Exception e) {
            log.error("Agent 處理失敗", e);
            return Flux.just("處理請求時出現錯誤: " + e.getMessage());
        }
    }

    /**
     * 獲取 Agent 能力描述
     */
    public Map<String, Object> getCapabilities() {
        Map<String, Object> capabilities = new HashMap<>();

        capabilities.put("agents", List.of(
            Map.of("name", "Tool Agent", "description", "處理外部系統操作（天氣、匯率、計算）"),
            Map.of("name", "RAG Agent", "description", "處理需要企業資料的問題"),
            Map.of("name", "Chat Agent", "description", "處理一般對話")
        ));

        capabilities.put("routingLogic", List.of(
            "Q1: 答案是否在外部系統？ → Tool Agent",
            "Q2: 答案是否在企業資料？ → RAG Agent（相關性 > 0.6）",
            "默認 → Chat Agent"
        ));

        return capabilities;
    }
}
