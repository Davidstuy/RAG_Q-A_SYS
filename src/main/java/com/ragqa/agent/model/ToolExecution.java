package com.ragqa.agent.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具執行結果
 */
@Data
public class ToolExecution {
    private boolean success;
    private Object result;
    private String errorMessage;
    private Map<String, Object> metadata;

    private ToolExecution() {}

    public static ToolExecution success(Object result) {
        ToolExecution execution = new ToolExecution();
        execution.success = true;
        execution.result = result;
        execution.metadata = new HashMap<>();
        return execution;
    }

    public static ToolExecution success(Object result, Map<String, Object> metadata) {
        ToolExecution execution = new ToolExecution();
        execution.success = true;
        execution.result = result;
        execution.metadata = metadata != null ? metadata : new HashMap<>();
        return execution;
    }

    public static ToolExecution error(String errorMessage) {
        ToolExecution execution = new ToolExecution();
        execution.success = false;
        execution.errorMessage = errorMessage;
        execution.metadata = new HashMap<>();
        return execution;
    }

    public String getResultAsString() {
        if (result == null) return "";
        return result.toString();
    }
}
