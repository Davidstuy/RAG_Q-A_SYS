package com.ragqa.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentDTO {
    private String fileName;
    private String contentType;
    private long fileSize;
    private String content;
    private int chunkCount;
}