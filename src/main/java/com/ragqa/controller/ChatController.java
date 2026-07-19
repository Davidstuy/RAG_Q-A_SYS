package com.ragqa.controller;

import com.ragqa.model.ChatHistory;
import com.ragqa.model.ChatRequest;
import com.ragqa.model.ChatResponse;
import com.ragqa.service.ChatService;
import com.ragqa.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final HistoryService historyService;
    
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
            Set<String> sessions = historyService.getAllSessionIds();
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResponseEntity.internalServerError()
                .body("获取会话列表失败: " + e.getMessage());
        }
    }
    

// TODO: 流式问答接口（待实现）
//    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> chatStream(@RequestBody ChatRequest request) {
//        return chatService.streamChat(request.getQuestion());
//    }
}