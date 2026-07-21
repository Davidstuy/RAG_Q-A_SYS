package com.ragqa.controller;

import com.ragqa.agent.model.AgentRequest;
import com.ragqa.agent.model.AgentResponse;
import com.ragqa.agent.service.AgentService;
import com.ragqa.agent.service.DialogueManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Agent 控制器
 * 提供智能 Agent 的 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AgentController {

    private final AgentService agentService;
    private final DialogueManager dialogueManager;

    /**
     * Agent 聊天接口
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody AgentRequest request) {
        log.info("收到 Agent 請求: {}", request.getMessage());

        // 如果沒有 sessionId，生成一個
        if (request.getSessionId() == null || request.getSessionId().isEmpty()) {
            request.setSessionId(UUID.randomUUID().toString());
        }

        AgentResponse response = agentService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取 Agent 能力
     */
    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        return ResponseEntity.ok(agentService.getCapabilities());
    }

    /**
     * 清除對話上下文
     */
    @DeleteMapping("/context/{sessionId}")
    public ResponseEntity<Map<String, String>> clearContext(@PathVariable String sessionId) {
        dialogueManager.clearContext(sessionId);
        return ResponseEntity.ok(Map.of("message", "對話上下文已清除"));
    }

    /**
     * 健康檢查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Agent Service",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
