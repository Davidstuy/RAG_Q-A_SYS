package com.ragqa.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatHistory {
    private Long id;
    private String sessionId;
    private String role;  // "user" or "assistant"
    private String content;
    private LocalDateTime createdAt;
}