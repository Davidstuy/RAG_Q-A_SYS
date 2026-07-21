package com.ragqa.agent.model;

import lombok.Data;

import java.util.Map;

/**
 * Agent 請求
 */
@Data
public class AgentRequest {
    private String sessionId;
    private String message;
    private Map<String, Object> context;
}
