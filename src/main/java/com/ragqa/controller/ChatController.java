package com.ragqa.controller;

import com.ragqa.agent.model.AgentRequest;
import com.ragqa.agent.model.AgentResponse;
import com.ragqa.agent.service.AgentService;
import com.ragqa.model.ChatHistory;
import com.ragqa.model.ChatRequest;
import com.ragqa.model.ChatResponse;
import com.ragqa.service.ChatService;
import com.ragqa.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final HistoryService historyService;
    private final AgentService agentService;
    
    /**
     * 问答接口（带会话ID）
     */
    @PostMapping
    public ResponseEntity<?> chat(
        @RequestBody ChatRequest request,
        @RequestParam(required = false) String sessionId
    ) {
        try {
            // 生成或使用现有会话ID
            if (sessionId == null || sessionId.isBlank()) {
                sessionId = UUID.randomUUID().toString();
            }
            
            // 验证输入
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                return ResponseEntity.badRequest().body("问题不能为空");
            }
            
            // 处理问答
            ChatResponse response = chatService.chat(request, sessionId);
            
            // 在响应头中返回会话ID
            return ResponseEntity.ok()
                .header("X-Session-Id", sessionId)
                .body(response);
            
        } catch (Exception e) {
            log.error("问答处理失败", e);
            return ResponseEntity.internalServerError()
                .body("问答处理失败: " + e.getMessage());
        }
    }

     /**
     * 获取对话历史
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(
        @PathVariable String sessionId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            List<ChatHistory> history = historyService.getHistory(sessionId, limit);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("获取历史失败", e);
            return ResponseEntity.internalServerError()
                .body("获取历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 清空对话历史
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<?> clearHistory(@PathVariable String sessionId) {
        try {
            historyService.clearHistory(sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("清空历史失败", e);
            return ResponseEntity.internalServerError()
                .body("清空历史失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有会话
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getAllSessions() {
        try {
            return ResponseEntity.ok(historyService.getAllSessions());
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResponseEntity.internalServerError()
                .body("获取会话列表失败: " + e.getMessage());
        }
    }

    /**
     * 重命名会话
     */
    @PutMapping("/sessions/{sessionId}/name")
    public ResponseEntity<?> renameSession(
        @PathVariable String sessionId,
        @RequestBody Map<String, String> request
    ) {
        try {
            String name = request.get("name");
            if (name == null || name.isBlank()) {
                return ResponseEntity.badRequest().body("名称不能为空");
            }
            historyService.setSessionName(sessionId, name.trim());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("重命名会话失败", e);
            return ResponseEntity.internalServerError()
                .body("重命名失败: " + e.getMessage());
        }
    }
    

    /**
     * 流式问答接口 - 使用 Agent 智能路由（流式输出）
     *
     * 教学：
     * - produces = TEXT_EVENT_STREAM_VALUE 表示这是一个 SSE（Server-Sent Events）端点
     * - SSE 是一种 HTTP 协议扩展，允许服务器持续向客户端推送数据
     * - 浏览器收到的数据格式：data: 你\n\ndata: 好\n\ndata: 吗\n\n
     * - Flux<String> 中的每个 String 元素会被自动包装为一个 SSE 事件
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
        @RequestBody ChatRequest request,
        @RequestParam(required = false) String sessionId
    ) {
        // 生成或使用现有会话ID
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 验证输入
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return Flux.just("问题不能为空");
        }

        // 使用 final 变量以便在 lambda 中使用
        final String finalSessionId = sessionId;

        // 使用 Agent 智能路由处理（流式）
        try {
            AgentRequest agentRequest = new AgentRequest();
            agentRequest.setSessionId(finalSessionId);
            agentRequest.setMessage(request.getQuestion());

            // 使用流式处理
            Flux<String> responseStream = agentService.streamChat(agentRequest);

            // 保存对话历史（在流完成后）
            // 使用 AtomicReference 来在 lambda 中修改变量
            final java.util.concurrent.atomic.AtomicReference<StringBuilder> fullResponseRef =
                new java.util.concurrent.atomic.AtomicReference<>(new StringBuilder());

            return responseStream
                .doOnNext(token -> {
                    fullResponseRef.get().append(token);
                })
                .doOnComplete(() -> {
                    // 流完成后保存对话历史
                    historyService.save(finalSessionId, "user", request.getQuestion());
                    historyService.save(finalSessionId, "assistant", fullResponseRef.get().toString());
                    log.info("流式完成，保存对话历史");
                });

        } catch (Exception e) {
            log.error("Agent 处理失败，回退到原始 RAG", e);
            // 回退到原始 RAG 流式处理
            return chatService.streamChat(request, finalSessionId);
        }
    }
}