package com.ragqa.model;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponse {
    private String answer;
    private List<Source> sources;
    private double relevanceScore;
    
    @Data
    public static class Source {
        private String content;
        private double score;
        private String metadata;
    }
}