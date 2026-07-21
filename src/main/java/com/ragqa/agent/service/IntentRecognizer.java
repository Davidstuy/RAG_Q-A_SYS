package com.ragqa.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragqa.agent.model.IntentResult;
import com.ragqa.agent.model.IntentType;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 意圖識別器
 * 使用 LLM 識別用戶意圖
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognizer {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 識別用戶意圖
     */
    public IntentResult recognize(String userInput, String context) {
        log.info("識別意圖: userInput={}", userInput);

        // 構建提示詞
        String prompt = buildIntentPrompt(userInput, context);

        // 調用 LLM 進行意圖識別
        List<ChatMessage> messages = List.of(
            SystemMessage.from("你是一個意圖識別助手。請分析用戶的輸入，識別其意圖，並返回 JSON 格式的結果。"),
            UserMessage.from(prompt)
        );

        try {
            AiMessage response = chatModel.generate(messages).content();
            String responseText = response.text();
            log.debug("意圖識別響應: {}", responseText);

            // 解析響應
            return parseIntentResponse(responseText);
        } catch (Exception e) {
            log.error("意圖識別失敗", e);
            return new IntentResult(IntentType.UNKNOWN, 0.0, new HashMap<>(), "識別失敗: " + e.getMessage());
        }
    }

    private String buildIntentPrompt(String userInput, String context) {
        return String.format("""
            請分析用戶的輸入，識別其意圖。

            用戶輸入: %s
            對話上下文: %s

            可用意圖類型:
            - WEATHER_QUERY: 查詢天氣相關信息（如：北京天氣、明天會下雨嗎）
            - CURRENCY_CONVERT: 貨幣匯率轉換（如：100美元等於多少人民幣）
            - DOCUMENT_SEARCH: 在文檔中搜索信息（如：什麼是RAG、幫我查一下...）
            - CALCULATION: 數學計算（如：2+3等於多少、計算100*50）
            - GENERAL_CHAT: 一般對話（如：你好、謝謝）
            - UNKNOWN: 無法識別的意圖

            請返回 JSON 格式的結果（不要包含其他文字）:
            {
                "intent": "意圖類型",
                "confidence": 0.95,
                "parameters": {
                    "city": "北京",
                    "date": "明天"
                },
                "explanation": "識別原因"
            }

            注意事項:
            1. 參數可以為空對象 {}
            2. confidence 範圍為 0-1
            3. 只返回 JSON，不要有其他內容
            """, userInput, context != null ? context : "無");
    }

    private IntentResult parseIntentResponse(String response) {
        try {
            // 嘗試提取 JSON（處理可能的 markdown 代碼塊）
            String json = extractJson(response);

            JsonNode rootNode = objectMapper.readTree(json);
            String intentStr = rootNode.get("intent").asText();
            double confidence = rootNode.get("confidence").asDouble();

            // 解析參數
            Map<String, Object> parameters = new HashMap<>();
            JsonNode paramsNode = rootNode.get("parameters");
            if (paramsNode != null && !paramsNode.isNull()) {
                parameters = objectMapper.convertValue(paramsNode,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class));
            }

            String explanation = rootNode.has("explanation") ? rootNode.get("explanation").asText() : "";

            // 轉換意圖類型
            IntentType intentType = parseIntentType(intentStr);

            return new IntentResult(intentType, confidence, parameters, explanation);
        } catch (Exception e) {
            log.error("解析意圖響應失敗: {}", response, e);
            return new IntentResult(IntentType.UNKNOWN, 0.0, new HashMap<>(), "解析失敗");
        }
    }

    private IntentType parseIntentType(String intentStr) {
        try {
            return IntentType.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 嘗試模糊匹配
            String normalized = intentStr.toUpperCase().replace(" ", "_");
            for (IntentType type : IntentType.values()) {
                if (type.name().contains(normalized) || normalized.contains(type.name())) {
                    return type;
                }
            }
            return IntentType.UNKNOWN;
        }
    }

    private String extractJson(String text) {
        // 嘗試提取 markdown 代碼塊中的 JSON
        int jsonStart = text.indexOf("```json");
        if (jsonStart != -1) {
            jsonStart = text.indexOf("\n", jsonStart) + 1;
            int jsonEnd = text.indexOf("```", jsonStart);
            if (jsonEnd != -1) {
                return text.substring(jsonStart, jsonEnd).trim();
            }
        }

        // 嘗試提取普通代碼塊
        jsonStart = text.indexOf("```");
        if (jsonStart != -1) {
            jsonStart = text.indexOf("\n", jsonStart) + 1;
            int jsonEnd = text.indexOf("```", jsonStart);
            if (jsonEnd != -1) {
                return text.substring(jsonStart, jsonEnd).trim();
            }
        }

        // 嘗試找到 JSON 對象
        int braceStart = text.indexOf("{");
        if (braceStart != -1) {
            int braceEnd = text.lastIndexOf("}");
            if (braceEnd > braceStart) {
                return text.substring(braceStart, braceEnd + 1);
            }
        }

        return text;
    }
}
