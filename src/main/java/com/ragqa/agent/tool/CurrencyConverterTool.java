package com.ragqa.agent.tool;

import com.ragqa.agent.model.IntentType;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 匯率轉換工具
 * 使用免費匯率 API 進行貨幣轉換
 */
@Slf4j
@Component
public class CurrencyConverterTool implements Tool {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.exchangerate-api.com/v4/latest")
            .build();

    // 常用貨幣代碼映射
    private static final Map<String, String> CURRENCY_MAP = Map.of(
        "人民幣", "CNY",
        "美元", "USD",
        "歐元", "EUR",
        "日元", "JPY",
        "英鎊", "GBP",
        "港幣", "HKD",
        "台幣", "TWD",
        "韓元", "KRW",
        "澳元", "AUD",
        "加元", "CAD"
    );

    @Override
    public String name() {
        return "currency_converter";
    }

    @Override
    public String description() {
        return "貨幣匯率轉換，支持多種貨幣之間的轉換";
    }

    @Override
    public IntentType intentType() {
        return IntentType.CURRENCY_CONVERT;
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("from", "源貨幣（如：USD、美元）", true),
            new ToolParameter("to", "目標貨幣（如：CNY、人民幣）", true),
            new ToolParameter("amount", "金額", true)
        );
    }

    @Override
    public ToolExecution execute(ToolInput input) {
        String from = input.getStringParameter("from");
        String to = input.getStringParameter("to");
        String amountStr = input.getStringParameter("amount");

        if (from == null || to == null || amountStr == null) {
            return ToolExecution.error("請提供完整的轉換信息：源貨幣、目標貨幣和金額");
        }

        // 轉換貨幣代碼
        from = normalizeCurrency(from);
        to = normalizeCurrency(to);

        try {
            double amount = Double.parseDouble(amountStr);
            log.info("匯率轉換: {} {} -> {}", amount, from, to);

            // 獲取匯率
            double rate = getExchangeRate(from, to);
            double result = amount * rate;

            String message = String.format(
                "%.2f %s = %.2f %s（匯率：1 %s = %.4f %s）",
                amount, from, result, to, from, rate, to
            );

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("from", from);
            metadata.put("to", to);
            metadata.put("amount", amount);
            metadata.put("rate", rate);
            metadata.put("result", result);

            return ToolExecution.success(message, metadata);

        } catch (NumberFormatException e) {
            return ToolExecution.error("金額格式錯誤: " + amountStr);
        } catch (Exception e) {
            log.error("匯率查詢失敗", e);
            return ToolExecution.error("匯率查詢失敗: " + e.getMessage());
        }
    }

    private String normalizeCurrency(String currency) {
        // 如果是中文名稱，轉換為代碼
        String upper = currency.toUpperCase();
        if (CURRENCY_MAP.containsKey(currency)) {
            return CURRENCY_MAP.get(currency);
        }
        // 檢查是否已經是代碼
        if (upper.matches("[A-Z]{3}")) {
            return upper;
        }
        return currency;
    }

    private double getExchangeRate(String from, String to) throws Exception {
        String response = webClient.get()
                .uri("/{from}", from)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // 解析匯率
        String rateKey = "\"" + to + "\":";
        int index = response.indexOf(rateKey);
        if (index == -1) {
            throw new RuntimeException("無法獲取 " + from + " 到 " + to + " 的匯率");
        }

        int start = index + rateKey.length();
        int end = response.indexOf(",", start);
        if (end == -1) {
            end = response.indexOf("}", start);
        }

        String rateStr = response.substring(start, end).trim();
        return Double.parseDouble(rateStr);
    }
}
