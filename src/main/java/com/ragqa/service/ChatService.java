package com.ragqa.service;

import com.ragqa.model.ChatRequest;
import com.ragqa.model.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatLanguageModel chatLanguageModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final HistoryService historyService;
    
    private static final int MAX_HISTORY = 5;  // 保留最近5轮对话
    
    /**
     * 处理问答请求
     */
    public ChatResponse chat(ChatRequest request, String sessionId) {
        log.info("收到问题: {}", request.getQuestion());
        
        // 1. 检索相关文档
        List<EmbeddingMatch<TextSegment>> relevantDocs = retrieveRelevantDocs(
            request.getQuestion(), 
            request.getTopK()
        );

         // 2. 加载对话历史
        List<com.ragqa.model.ChatHistory> history = 
            historyService.getHistory(sessionId, MAX_HISTORY);
        
        //  构建上下文
        String context = buildContext(relevantDocs);
        
        //  构建 Prompt（包含历史）
        String prompt = buildPromptWithHistory(
            request.getQuestion(), 
            context, 
            history
        );
        
        // 调用 LLM 生成回答
        String answer = generateAnswer(prompt);

        //  保存对话历史
        historyService.save(sessionId, "user", request.getQuestion());
        historyService.save(sessionId, "assistant", answer);
        
        
        // 构建响应
        ChatResponse response = new ChatResponse();
        response.setAnswer(answer);
        response.setSources(buildSources(relevantDocs));
        response.setRelevanceScore(calculateRelevance(relevantDocs));
        
        return response;
    }
    
    /**
     * 检索相关文档
     */
    private List<EmbeddingMatch<TextSegment>> retrieveRelevantDocs(String question, int topK) {
        // 将问题向量化 - embed() 返回 Response<Embedding>，content() 返回 Embedding 对象
        // Embedding 内部封装了 float[] 向量，但 findRelevant() 需要 Embedding 类型
        Embedding questionEmbedding = embeddingModel.embed(question).content();

        // 在向量库中搜索最相关的文档
        return embeddingStore.findRelevant(questionEmbedding, topK);
    }
    
    /**
     * 构建上下文
     */
    private String buildContext(List<EmbeddingMatch<TextSegment>> relevantDocs) {
        return relevantDocs.stream()
            .map(match -> match.embedded().text())
            .collect(Collectors.joining("\n\n---\n\n"));
    }
    
    /**
     * 构建 Prompt
     */
     /**
     * 构建 Prompt（包含历史）
     */
    private String buildPromptWithHistory(
        String question, 
        String context, 
        List<com.ragqa.model.ChatHistory> history
    ) {
        StringBuilder sb = new StringBuilder();
        
        // 系统提示
        sb.append("你是一个智能问答助手，擅长基于技术文档回答问题。\n\n");
        sb.append("# 规则\n");
        sb.append("1. 仅根据提供的参考文档回答问题\n");
        sb.append("2. 如果文档中没有答案，请诚实说明\n");
        sb.append("3. 回答要准确、简洁、易懂\n");
        sb.append("4. 保持客观中立的语气\n");
        sb.append("5. 基于对话历史理解上下文\n\n");
        
        // 对话历史
        if (!history.isEmpty()) {
            sb.append("# 对话历史\n");
            for (com.ragqa.model.ChatHistory h : history) {
                sb.append(h.getRole()).append(": ").append(h.getContent()).append("\n");
            }
            sb.append("\n");
        }
        
        // 参考文档
        sb.append("# 参考文档\n");
        sb.append(context).append("\n\n");
        
        // 当前问题
        sb.append("# 用户问题\n");
        sb.append(question);
        
        return sb.toString();
    }
    
    
    /**
     * 生成回答
     */
    private String generateAnswer(String prompt) {
        // 构建消息列表
        List<dev.langchain4j.data.message.ChatMessage> messages = List.of(
            SystemMessage.from(prompt),
            UserMessage.from("请回答问题")
        );
        
        // 调用 LLM
        AiMessage response = chatLanguageModel.generate(messages).content();
        return response.text();
    }
    
    /**
     * 构建来源信息
     */
    private List<ChatResponse.Source> buildSources(List<EmbeddingMatch<TextSegment>> relevantDocs) {
        return relevantDocs.stream()
            .map(match -> {
                ChatResponse.Source source = new ChatResponse.Source();
                source.setContent(match.embedded().text());
                source.setScore(match.score());
                source.setMetadata(match.embedded().metadata().toString());
                return source;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 计算相关性得分
     */
    private double calculateRelevance(List<EmbeddingMatch<TextSegment>> relevantDocs) {
        if (relevantDocs.isEmpty()) {
            return 0.0;
        }
        return relevantDocs.stream()
            .mapToDouble(EmbeddingMatch::score)
            .average()
            .orElse(0.0);
    }
}