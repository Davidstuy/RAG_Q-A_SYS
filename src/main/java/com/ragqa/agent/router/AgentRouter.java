package com.ragqa.agent.router;

import com.ragqa.agent.model.IntentType;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;
import com.ragqa.agent.tool.Tool;
import com.ragqa.agent.tool.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Agent Router - 二元決策路由
 *
 * 流程：
 * Q1: 答案是否在外部系統？ → Tool Agent
 * Q2: 答案是否在企業資料？ → RAG 檢索判斷 → RAG Agent 或 Chat Agent
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentRouter {

    private final ToolRegistry toolRegistry;

    // Q1: 外部系統關鍵詞
    private static final Pattern EXTERNAL_SYSTEM_PATTERN = Pattern.compile(
        "(天氣|weather|溫度|下雨|氣溫|" +
        "匯率|exchange|美元|人民幣|歐元|日元|換算|currency|USD|CNY|EUR|JPY|GBP|" +
        "計算|calculate|算一下|等於多少|\\d+\\s*[+\\-*/]\\s*\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    // Chat Agent: 問候語、簡單對話
    private static final Pattern CHAT_PATTERN = Pattern.compile(
        "^(hi|hello|hey|你好|嗨|哈囉|早安|午安|晚安|" +
        "謝謝|thanks|thank you|好的|ok|okay|了解|知道了|明白|沒事|再見|bye|拜拜).*$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 路由決策
     * @return AgentType: TOOL, RAG, CHAT
     */
    public RouteResult route(String question) {
        log.info("路由決策: question={}", question);

        // Q0: 是否為簡單對話？（問候語、簡短回應）
        if (isSimpleChat(question)) {
            log.info("Q0=Yes → Chat Agent（簡單對話）");
            return RouteResult.chat();
        }

        // Q1: 是否需要操作外部系統？
        if (isExternalSystemQuestion(question)) {
            IntentType toolType = detectToolType(question);
            log.info("Q1=Yes → Tool Agent ({})", toolType);
            return RouteResult.tool(toolType);
        }

        // Q2: 是否需要企業資料？→ 先嘗試 RAG 檢索
        log.info("Q1=No → 需要檢測企業資料");
        return RouteResult.ragCheck(); // 需要後續 RAG 檢索判斷
    }

    /**
     * 判斷是否為簡單對話
     */
    private boolean isSimpleChat(String question) {
        // 問候語、簡短回應
        if (CHAT_PATTERN.matcher(question.trim()).matches()) {
            return true;
        }
        // 短問題（< 10個字）且不是疑問句
        if (question.length() < 10 && !question.contains("?") && !question.contains("？")) {
            return true;
        }
        return false;
    }

    /**
     * Q1: 判斷是否為外部系統問題
     */
    private boolean isExternalSystemQuestion(String question) {
        return EXTERNAL_SYSTEM_PATTERN.matcher(question).find();
    }

    /**
     * 檢測具體工具類型
     */
    private IntentType detectToolType(String question) {
        String lower = question.toLowerCase();

        // 天氣查詢
        if (lower.contains("天氣") || lower.contains("weather") || lower.contains("氣溫") ||
            lower.contains("temperature")) {
            return IntentType.WEATHER_QUERY;
        }

        // 匯率轉換
        if (lower.contains("匯率") || lower.contains("exchange") || lower.contains("currency") ||
            lower.contains("美元") || lower.contains("人民幣") || lower.contains("歐元") ||
            lower.contains("日元") || lower.contains("英鎊") ||
            lower.contains("usd") || lower.contains("cny") || lower.contains("eur") ||
            lower.contains("jpy") || lower.contains("gbp") ||
            lower.matches(".*\\d+\\s*(usd|cny|eur|jpy|gbp|dollars?|yuan|renminbi).*") ||
            lower.matches(".*(to|換|convert).*\\d+.*")) {
            return IntentType.CURRENCY_CONVERT;
        }

        // 數學計算
        if (lower.contains("計算") || lower.contains("calculate") || lower.contains("算一下") ||
            lower.matches(".*\\d+\\s*[+\\-*/]\\s*\\d+.*") ||
            lower.matches(".*\\d+\\s*(plus|minus|times|divided).*\\d+.*")) {
            return IntentType.CALCULATION;
        }

        return IntentType.UNKNOWN;
    }

    /**
     * 路由結果
     */
    public static class RouteResult {
        private final RouteType type;
        private final IntentType toolType;

        private RouteResult(RouteType type, IntentType toolType) {
            this.type = type;
            this.toolType = toolType;
        }

        public static RouteResult tool(IntentType toolType) {
            return new RouteResult(RouteType.TOOL, toolType);
        }

        public static RouteResult ragCheck() {
            return new RouteResult(RouteType.RAG_CHECK, null);
        }

        public static RouteResult rag() {
            return new RouteResult(RouteType.RAG, null);
        }

        public static RouteResult chat() {
            return new RouteResult(RouteType.CHAT, null);
        }

        public RouteType getType() { return type; }
        public IntentType getToolType() { return toolType; }
    }

    public enum RouteType {
        TOOL,       // 工具 Agent
        RAG_CHECK,  // 需要 RAG 檢索判斷
        RAG,        // RAG Agent
        CHAT        // Chat Agent
    }
}
