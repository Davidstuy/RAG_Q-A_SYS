package com.ragqa.agent.model;

import lombok.Data;

import java.util.Map;

/**
 * Agent 響應
 */
@Data
public class AgentResponse {
    private boolean success;
    private String message;
    private Object data;
    private String type;
    private Map<String, Object> metadata;

    public static AgentResponse success(Object data) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setType("SUCCESS");
        return response;
    }

    public static AgentResponse success(Object data, String message) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setMessage(message);
        response.setType("SUCCESS");
        return response;
    }

    public static AgentResponse parameterNeeded(String message) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setType("PARAMETER_NEEDED");
        return response;
    }

    public static AgentResponse error(String message) {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setType("ERROR");
        return response;
    }

    public static AgentResponse intentNotRecognized() {
        AgentResponse response = new AgentResponse();
        response.setSuccess(false);
        response.setMessage("抱歉，我無法理解您的意圖，請換一種方式描述。");
        response.setType("INTENT_NOT_RECOGNIZED");
        return response;
    }
}
