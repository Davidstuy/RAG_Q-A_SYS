package com.ragqa.agent.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具輸入
 */
@Data
public class ToolInput {
    private Map<String, Object> parameters = new HashMap<>();
    private String sessionId;
    private List<String> context;

    public ToolInput() {}

    public ToolInput(Map<String, Object> parameters, String sessionId) {
        this.parameters = parameters;
        this.sessionId = sessionId;
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {
        return (T) parameters.get(key);
    }

    public String getStringParameter(String key) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : null;
    }

    public String getStringParameter(String key, String defaultValue) {
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
