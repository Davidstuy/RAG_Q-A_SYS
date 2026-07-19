package com.ragqa.service;

import com.ragqa.model.ChatHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class HistoryService {
    
    // 使用内存存储（生产环境应使用 Redis/数据库）
    private final Map<String, List<ChatHistory>> historyStore = new ConcurrentHashMap<>();
    
    /**
     * 保存对话
     */
    public void save(String sessionId, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        history.setCreatedAt(LocalDateTime.now());
        
        historyStore.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(history);
        
        log.debug("保存对话: session={}, role={}, content={}", sessionId, role, content);
    }
    
    /**
     * 获取对话历史
     */
    public List<ChatHistory> getHistory(String sessionId, int limit) {
        List<ChatHistory> history = historyStore.getOrDefault(sessionId, new ArrayList<>());
        
        // 返回最近的 N 条
        int start = Math.max(0, history.size() - limit);
        return history.subList(start, history.size());
    }
    
    /**
     * 清空对话历史
     */
    public void clearHistory(String sessionId) {
        historyStore.remove(sessionId);
        log.info("清空对话历史: session={}", sessionId);
    }
    
    /**
     * 获取所有会话ID
     */
    public Set<String> getAllSessionIds() {
        return historyStore.keySet();
    }
}