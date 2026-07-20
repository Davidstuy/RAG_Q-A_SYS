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
    // 会话名称映射 (sessionId -> displayName)
    private final Map<String, String> sessionNames = new ConcurrentHashMap<>();

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

    /**
     * 获取会话名称
     */
    public String getSessionName(String sessionId) {
        return sessionNames.getOrDefault(sessionId, "新對話");
    }

    /**
     * 设置会话名称
     */
    public void setSessionName(String sessionId, String name) {
        sessionNames.put(sessionId, name);
        log.info("更新会话名称: session={}, name={}", sessionId, name);
    }

    /**
     * 获取所有会话信息（ID + 名称）
     */
    public List<Map<String, String>> getAllSessions() {
        List<Map<String, String>> sessions = new ArrayList<>();
        for (String sid : historyStore.keySet()) {
            Map<String, String> session = new HashMap<>();
            session.put("id", sid);
            session.put("name", getSessionName(sid));
            sessions.add(session);
        }
        return sessions;
    }
}