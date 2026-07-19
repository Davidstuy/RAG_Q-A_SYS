package com.ragqa.service;

import com.ragqa.model.DocumentDTO;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    /**
     * 处理上传的文档
     */
    public DocumentDTO processDocument(MultipartFile file) throws IOException {
        log.info("开始处理文档: {}", file.getOriginalFilename());

        // 1. 提取文本
        String text = extractText(file);

        // 2. 创建 Document 对象
        Document document = Document.from(text);

        // 3. 分块
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100);
        List<TextSegment> segments = splitter.split(document);

        // 4. 向量化并存储到 EmbeddingStore
        for (TextSegment segment : segments) {
            Embedding embedding = embeddingModel.embed(segment).content();
            embeddingStore.add(embedding, segment);
        }

        // 5. 返回结果
        DocumentDTO dto = new DocumentDTO();
        dto.setFileName(file.getOriginalFilename());
        dto.setContentType(file.getContentType());
        dto.setFileSize(file.getSize());
        dto.setContent(text);
        dto.setChunkCount(segments.size());

        log.info("文档处理完成，共 {} 个块", segments.size());
        return dto;
    }

    /**
     * 从文件中提取文本
     */
    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType != null && contentType.equals("application/pdf")) {
            return extractFromPDF(file);
        } else {
            // 其他格式（纯文本）
            return new String(file.getBytes());
        }
    }

    /**
     * 从 PDF 提取文本
     */
    private String extractFromPDF(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
