package com.ragqa.agent.tool;

import com.ragqa.agent.model.IntentType;
import com.ragqa.agent.model.ToolExecution;
import com.ragqa.agent.model.ToolInput;
import com.ragqa.model.ChatRequest;
import com.ragqa.model.ChatResponse;
import com.ragqa.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文檔檢索工具
 * 基於現有的 RAG 服務進行文檔搜索
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSearchTool implements Tool {

    private final ChatService chatService;

    @Override
    public String name() {
        return "document_search";
    }

    @Override
    public String description() {
        return "在已上傳的文檔中搜索信息，基於語義檢索找到最相關的內容";
    }

    @Override
    public IntentType intentType() {
        return IntentType.DOCUMENT_SEARCH;
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
            new ToolParameter("query", "搜索查詢", true),
            new ToolParameter("top_k", "返回結果數量", false, "5")
        );
    }

    @Override
    public ToolExecution execute(ToolInput input) {
        String query = input.getStringParameter("query");
        String topKStr = input.getStringParameter("top_k", "5");

        if (query == null || query.isBlank()) {
            return ToolExecution.error("請提供搜索內容");
        }

        try {
            int topK = Integer.parseInt(topKStr);
            log.info("文檔檢索: query={}, topK={}", query, topK);

            // 使用現有的 ChatService 進行檢索
            ChatRequest request = new ChatRequest();
            request.setQuestion(query);
            request.setTopK(topK);

            ChatResponse response = chatService.chat(request, input.getSessionId());

            // 構建結果
            StringBuilder result = new StringBuilder();
            result.append("搜索結果：\n\n");
            result.append(response.getAnswer());

            if (response.getSources() != null && !response.getSources().isEmpty()) {
                result.append("\n\n參考來源：\n");
                for (int i = 0; i < response.getSources().size(); i++) {
                    ChatResponse.Source source = response.getSources().get(i);
                    result.append(String.format("%d. %s (相關度: %.2f)\n",
                        i + 1,
                        truncate(source.getContent(), 100),
                        source.getScore()));
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("query", query);
            metadata.put("topK", topK);
            metadata.put("relevanceScore", response.getRelevanceScore());
            metadata.put("sourceCount", response.getSources() != null ? response.getSources().size() : 0);

            return ToolExecution.success(result.toString(), metadata);

        } catch (NumberFormatException e) {
            return ToolExecution.error("top_k 參數格式錯誤");
        } catch (Exception e) {
            log.error("文檔檢索失敗", e);
            return ToolExecution.error("文檔檢索失敗: " + e.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
