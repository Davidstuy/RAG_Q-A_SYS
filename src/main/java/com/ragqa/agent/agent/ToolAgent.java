package com.ragqa.agent.agent;

import com.ragqa.agent.model.*;
import com.ragqa.agent.tool.Tool;
import com.ragqa.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool Agent - 處理外部系統操作
 *
 * 職責：
 * - 天氣查詢
 * - 匯率轉換
 * - 數學計算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolAgent {

    private final ToolRegistry toolRegistry;

    /**
     * 處理工具類問題
     */
    public AgentResponse chat(String userMessage, IntentType toolType) {
        log.info("Tool Agent 處理: type={}, message={}", toolType, userMessage);

        // 1. 獲取對應工具
        Tool tool = toolRegistry.getToolForIntent(toolType);
        if (tool == null) {
            return AgentResponse.error("找不到合適的工具");
        }

        // 2. 提取參數
        Map<String, Object> parameters = extractParameters(userMessage, toolType);

        // 3. 構建工具輸入
        ToolInput toolInput = new ToolInput();
        toolInput.setParameters(parameters);

        // 4. 檢查參數完整性
        if (!tool.validateParameters(toolInput)) {
            String missingParams = tool.parameters().stream()
                    .filter(p -> p.required() && !parameters.containsKey(p.name()))
                    .map(Tool.ToolParameter::description)
                    .collect(Collectors.joining(", "));
            return AgentResponse.parameterNeeded("請提供以下信息: " + missingParams);
        }

        // 5. 執行工具
        log.info("執行工具: {}", tool.name());
        ToolExecution execution = tool.execute(toolInput);

        if (execution.isSuccess()) {
            return AgentResponse.success(execution.getResult(), execution.getResultAsString());
        } else {
            return AgentResponse.error(execution.getErrorMessage());
        }
    }

    /**
     * 提取工具參數
     */
    private Map<String, Object> extractParameters(String question, IntentType toolType) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();

        switch (toolType) {
            case WEATHER_QUERY:
                params.put("city", extractCity(question));
                break;
            case CURRENCY_CONVERT:
                params.putAll(extractCurrencyParams(question));
                break;
            case CALCULATION:
                params.put("expression", extractCalcExpression(question));
                break;
        }

        return params;
    }

    /**
     * 提取城市名稱
     */
    private String extractCity(String question) {
        // 移除天氣相關詞語，提取城市
        String city = question.replaceAll("(天氣|weather|溫度|下雨|晴天|氣溫|怎麼樣|如何|查詢|查一下|今天|明天|後天|in|the)", "").trim();
        return city.isEmpty() ? "北京" : city;
    }

    /**
     * 提取匯率參數
     */
    private Map<String, Object> extractCurrencyParams(String question) {
        Map<String, Object> params = new java.util.HashMap<>();

        // 默認值
        String from = "USD";
        String to = "CNY";
        String amount = "100";

        // 嘗試提取金額
        Matcher amountMatcher = Pattern.compile("(\\d+)").matcher(question);
        if (amountMatcher.find()) {
            amount = amountMatcher.group(1);
        }

        // 嘗試提取貨幣
        String upper = question.toUpperCase();
        if (upper.contains("USD") || upper.contains("美元") || upper.contains("DOLLAR")) {
            from = "USD";
        } else if (upper.contains("EUR") || upper.contains("歐元") || upper.contains("EURO")) {
            from = "EUR";
        } else if (upper.contains("JPY") || upper.contains("日元") || upper.contains("YEN")) {
            from = "JPY";
        } else if (upper.contains("GBP") || upper.contains("英鎊") || upper.contains("POUND")) {
            from = "GBP";
        }

        if (upper.contains("CNY") || upper.contains("人民幣") || upper.contains("YUAN") || upper.contains("RENMINBI")) {
            to = "CNY";
        } else if (upper.contains("USD") || upper.contains("美元") || upper.contains("DOLLAR")) {
            to = "USD";
        } else if (upper.contains("EUR") || upper.contains("歐元") || upper.contains("EURO")) {
            to = "EUR";
        } else if (upper.contains("JPY") || upper.contains("日元") || upper.contains("YEN")) {
            to = "JPY";
        }

        params.put("from", from);
        params.put("to", to);
        params.put("amount", amount);

        return params;
    }

    /**
     * 提取計算表達式
     */
    private String extractCalcExpression(String question) {
        // 嘗試提取數學表達式
        Matcher matcher = Pattern.compile("(\\d+\\s*[+\\-*/]\\s*\\d+)").matcher(question);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return question;
    }
}
