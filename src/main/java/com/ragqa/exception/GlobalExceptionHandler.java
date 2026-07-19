package com.ragqa.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse("参数错误", e.getMessage()));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException e) {
        log.error("运行时错误", e);
        return ResponseEntity.internalServerError()
            .body(errorResponse("服务器错误", "请联系管理员"));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        log.error("未知错误", e);
        return ResponseEntity.internalServerError()
            .body(errorResponse("系统错误", "请联系管理员"));
    }
    
    private Map<String, Object> errorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", error);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}