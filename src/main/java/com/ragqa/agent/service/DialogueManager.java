package com.ragqa.agent.service;

import com.ragqa.agent.model.IntentResult;
import com.ragqa.agent.model.IntentType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 對話管理器
 * 管理對話上下文和狀態
 */
@Slf4j
@Component
public class DialogueManager {

    private final Map<String, DialogueContext> contexts = new ConcurrentHashMap<>();

    /**
     * 獲取對話上下文
     */
    public DialogueContext getContext(String sessionId) {
        return contexts.computeIfAbsent(sessionId, id -> {
            log.info("創建新的對話上下文: {}", id);
            return new DialogueContext(id);
        });
    }

    /**
     * 更新對話上下文
     */
    public void updateContext(String sessionId, String userInput, String response, IntentResult intent) {
        DialogueContext context = getContext(sessionId);
        context.addTurn(userInput, response, intent);
        context.updateLastActivity();
        log.debug("更新對話上下文: sessionId={}, turns={}", sessionId, context.getTurns().size());
    }

    /**
     * 獲取上下文文本
     */
    public String getContextText(String sessionId) {
        DialogueContext context = getContext(sessionId);
        return context.getContextText();
    }

    /**
     * 清除對話上下文
     */
    public void clearContext(String sessionId) {
        contexts.remove(sessionId);
        log.info("清除對話上下文: {}", sessionId);
    }

    /**
     * 獲取最後一個意圖
     */
    public IntentType getLastIntent(String sessionId) {
        DialogueContext context = getContext(sessionId);
        return context.getLastIntent();
    }

    /**
     * 清理過期的對話上下文（超過30分鐘）
     */
    public void cleanExpiredContexts() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<String> expiredSessions = contexts.entrySet().stream()
                .filter(entry -> entry.getValue().getLastActivity().isBefore(threshold))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        expiredSessions.forEach(contexts::remove);
        if (!expiredSessions.isEmpty()) {
            log.info("清理過期對話上下文: {} 個", expiredSessions.size());
        }
    }

    /**
     * 對話上下文
     */
    @Data
    public static class DialogueContext {
        private String sessionId;
        private List<DialogueTurn> turns = new ArrayList<>();
        private LocalDateTime lastActivity;
        private Map<String, Object> pendingParameters = new ConcurrentHashMap<>();

        public DialogueContext(String sessionId) {
            this.sessionId = sessionId;
            this.lastActivity = LocalDateTime.now();
        }

        public void addTurn(String userInput, String response, IntentResult intent) {
            DialogueTurn turn = new DialogueTurn();
            turn.setUserInput(userInput);
            turn.setResponse(response);
            turn.setTimestamp(LocalDateTime.now());
            turn.setIntent(intent != null ? intent.getIntent() : null);
            turn.setParameters(intent != null ? intent.getParameters() : null);

            turns.add(turn);

            // 保持最近10輪對話
            if (turns.size() > 10) {
                turns = new ArrayList<>(turns.subList(turns.size() - 10, turns.size()));
            }
        }

        public void updateLastActivity() {
            this.lastActivity = LocalDateTime.now();
        }

        public String getContextText() {
            return turns.stream()
                    .map(turn -> "用戶: " + turn.getUserInput() + "\n助手: " + turn.getResponse())
                    .collect(Collectors.joining("\n\n"));
        }

        public IntentType getLastIntent() {
            if (turns.isEmpty()) {
                return null;
            }
            return turns.get(turns.size() - 1).getIntent();
        }

        public void setPendingParameter(String key, Object value) {
            pendingParameters.put(key, value);
        }

        public Object getPendingParameter(String key) {
            return pendingParameters.get(key);
        }

        public void clearPendingParameters() {
            pendingParameters.clear();
        }
    }

    /**
     * 對話輪次
     */
    @Data
    public static class DialogueTurn {
        private String userInput;
        private String response;
        private LocalDateTime timestamp;
        private IntentType intent;
        private Map<String, Object> parameters;
    }
}
