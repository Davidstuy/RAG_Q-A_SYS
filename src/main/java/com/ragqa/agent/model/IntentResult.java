package com.ragqa.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 意圖識別結果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IntentResult {
    private IntentType intent;
    private double confidence;
    private Map<String, Object> parameters;
    private String explanation;

    /**
     * 判斷是否為高置信度識別
     */
    public boolean isConfident() {
        return confidence >= 0.7;
    }

    /**
     * 獲取參數（帶默認值）
     */
    public String getParameter(String key, String defaultValue) {
        if (parameters == null || !parameters.containsKey(key)) {
            return defaultValue;
        }
        Object value = parameters.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
