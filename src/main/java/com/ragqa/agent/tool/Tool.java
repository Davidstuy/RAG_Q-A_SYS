package com.ragqa.agent.tool;

import com.ragqa.agent.model.IntentType;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;

import java.util.List;
import java.util.Map;

/**
 * 工具接口
 * 所有 Agent 工具都需要實現此接口
 */
public interface Tool {
    /**
     * 工具名稱
     */
    String name();

    /**
     * 工具描述
     */
    String description();

    /**
     * 對應的意圖類型
     */
    IntentType intentType();

    /**
     * 執行工具
     */
    ToolExecution execute(ToolInput input);

    /**
     * 工具參數定義
     */
    List<ToolParameter> parameters();

    /**
     * 檢查參數是否完整
     */
    default boolean validateParameters(ToolInput input) {
        for (ToolParameter param : parameters()) {
            if (param.required() && !input.getParameters().containsKey(param.name())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 參數定義
     */
    record ToolParameter(String name, String description, boolean required, String defaultValue) {
        public ToolParameter(String name, String description, boolean required) {
            this(name, description, required, null);
        }
    }
}
