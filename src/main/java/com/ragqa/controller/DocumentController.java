package com.ragqa.controller;

import com.ragqa.model.DocumentDTO;
import com.ragqa.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件不能为空");
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("文件大小不能超过 10MB");
            }

            // 处理文档
            DocumentDTO result = documentService.processDocument(file);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("文档处理失败", e);
            return ResponseEntity.internalServerError()
                    .body("文档处理失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document service is running");
    }
}