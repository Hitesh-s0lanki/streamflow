package com.streamflow.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps application exceptions to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(S3UploadException.class)
    public ResponseEntity<Map<String, Object>> handleS3UploadException(S3UploadException ex) {
        log.warn("S3 upload error: {} (bucket={}, key={})", ex.getMessage(), ex.getBucket(), ex.getKey());
        boolean clientError = ex.getMessage() != null && (
                ex.getMessage().contains("not configured")
                        || ex.getMessage().contains("null or empty")
                        || ex.getMessage().contains("exceeds maximum"));
        HttpStatus status = clientError ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(status)
                .body(Map.of(
                        "error", "S3 upload failed",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                        "bucket", ex.getBucket() != null ? ex.getBucket() : "",
                        "key", ex.getKey() != null ? ex.getKey() : ""));
    }

    @ExceptionHandler(S3StorageException.class)
    public ResponseEntity<Map<String, Object>> handleS3StorageException(S3StorageException ex) {
        log.warn("S3 storage error: {} (bucket={}, key={})", ex.getMessage(), ex.getBucket(), ex.getKey());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "S3 storage failed",
                        "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                        "bucket", ex.getBucket() != null ? ex.getBucket() : "",
                        "key", ex.getKey() != null ? ex.getKey() : ""));
    }
}
