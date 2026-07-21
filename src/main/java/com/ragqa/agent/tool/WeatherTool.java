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
 * 天氣查詢工具
 * 使用免費天氣 API 查詢城市天氣
 */
@Slf4j
@Component
public class WeatherTool implements Tool {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://wttr.in")
            .build();

    @Override
    public String name() {
        return "weather_query";
    }

    @Override
    public String description() {
        return "查詢指定城市的天氣信息，包括溫度、天氣狀況等";
    }

    @Override
    public IntentType intentType() {
        return IntentType.WEATHER_QUERY;
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("city", "城市名稱", true),
            new ToolParameter("date", "日期（今天/明天/後天）", false, "今天")
        );
    }

    @Override
    public ToolExecution execute(ToolInput input) {
        String city = input.getStringParameter("city");
        String date = input.getStringParameter("date", "今天");

        if (city == null || city.isBlank()) {
            return ToolExecution.error("請提供城市名稱");
        }

        log.info("查詢天氣: city={}, date={}", city, date);

        try {
            // 使用 wttr.in 免費 API
            String response = webClient.get()
                    .uri("/{city}?format=j1&lang=zh", city)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析天氣信息
            String weatherInfo = parseWeatherResponse(response, city, date);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("city", city);
            metadata.put("date", date);
            metadata.put("source", "wttr.in");

            return ToolExecution.success(weatherInfo, metadata);

        } catch (Exception e) {
            log.error("天氣查詢失敗", e);
            return ToolExecution.error("天氣查詢失敗: " + e.getMessage());
        }
    }

    private String parseWeatherResponse(String response, String city, String date) {
        // 簡單解析 JSON 響應
        // 實際項目中應使用 Jackson 或 Gson 進行完整解析
        try {
            // 提取溫度和天氣描述
            String temp = extractValue(response, "\"temp_C\":\"", "\"");
            String desc = extractValue(response, "\"weatherDesc\":\"", "\"");
            String humidity = extractValue(response, "\"humidity\":\"", "\"");
            String windSpeed = extractValue(response, "\"windspeedKmph\":\"", "\"");

            if (temp == null) temp = "未知";
            if (desc == null) desc = "未知";
            if (humidity == null) humidity = "未知";
            if (windSpeed == null) windSpeed = "未知";

            return String.format(
                "%s%s天氣：溫度 %s°C，%s，濕度 %s%%，風速 %s km/h",
                city, date, temp, desc, humidity, windSpeed
            );
        } catch (Exception e) {
            log.warn("解析天氣響應失敗", e);
            return city + date + "天氣信息獲取成功，但解析失敗";
        }
    }

    private String extractValue(String json, String startKey, String endKey) {
        int start = json.indexOf(startKey);
        if (start == -1) return null;
        start += startKey.length();
        int end = json.indexOf(endKey, start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
