package com.ragqa.model;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private int topK = 5;  // 返回前5个最相关的文档块
}