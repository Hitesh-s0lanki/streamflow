package com.streamflow.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps application exceptions to HTTP responses. All responses include
 * correlationId when present (from request correlation filter).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
        private static final String CORRELATION_ID = "correlationId";

        private static Map<String, Object> withCorrelation(Map<String, Object> body) {
                String cid = MDC.get(CORRELATION_ID);
                if (cid != null && !cid.isBlank()) {
                        body = new LinkedHashMap<>(body);
                        body.put(CORRELATION_ID, cid);
                }
                return body;
        }

        @ExceptionHandler(S3UploadException.class)
        public ResponseEntity<Map<String, Object>> handleS3UploadException(S3UploadException ex) {
                log.warn("S3 upload error: {} (bucket={}, key={})", ex.getMessage(), ex.getBucket(), ex.getKey());
                boolean clientError = ex.getMessage() != null && (ex.getMessage().contains("not configured")
                                || ex.getMessage().contains("null or empty")
                                || ex.getMessage().contains("exceeds maximum"));
                HttpStatus status = clientError ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                Map<String, Object> body = Map.of(
                                "error", "S3 upload failed",
                                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                                "bucket", ex.getBucket() != null ? ex.getBucket() : "",
                                "key", ex.getKey() != null ? ex.getKey() : "");
                return ResponseEntity.status(status).body(withCorrelation(body));
        }

        @ExceptionHandler(S3StorageException.class)
        public ResponseEntity<Map<String, Object>> handleS3StorageException(S3StorageException ex) {
                log.warn("S3 storage error: {} (bucket={}, key={})", ex.getMessage(), ex.getBucket(), ex.getKey());
                Map<String, Object> body = Map.of(
                                "error", "S3 storage failed",
                                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                                "bucket", ex.getBucket() != null ? ex.getBucket() : "",
                                "key", ex.getKey() != null ? ex.getKey() : "");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(withCorrelation(body));
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
                log.debug("Resource not found: {} {}", ex.getResourceType(), ex.getId());
                Map<String, Object> body = Map.of(
                                "error", "Not found",
                                "message", ex.getMessage(),
                                "resourceType", ex.getResourceType(),
                                "id", ex.getId().toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(withCorrelation(body));
        }

        @ExceptionHandler(BadRequestException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
                log.warn("Bad request: {}", ex.getMessage());
                Map<String, Object> body = Map.of("error", "Bad request", "message", ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(withCorrelation(body));
        }

        @ExceptionHandler(ConflictException.class)
        public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
                log.warn("Conflict: {}", ex.getMessage());
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "Conflict");
                body.put("message", ex.getMessage());
                if (ex.getResourceType() != null)
                        body.put("resourceType", ex.getResourceType());
                if (ex.getId() != null)
                        body.put("id", ex.getId().toString());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(withCorrelation(body));
        }

        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
                log.warn("Forbidden: {}", ex.getMessage());
                Map<String, Object> body = Map.of("error", "Forbidden", "message", ex.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(withCorrelation(body));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
                String errors = ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .collect(Collectors.joining("; "));
                log.warn("Validation failed: {}", errors);
                Map<String, Object> body = Map.of("error", "Validation failed", "message", errors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(withCorrelation(body));
        }

        @ExceptionHandler(VideoProcessingException.class)
        public ResponseEntity<Map<String, Object>> handleVideoProcessingException(VideoProcessingException ex) {
                log.warn("Video processing error: {}", ex.getMessage());
                Map<String, Object> body = Map.of(
                                "error", "Video processing failed",
                                "message", ex.getMessage() != null ? ex.getMessage() : "Unknown error");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(withCorrelation(body));
        }

        @ExceptionHandler(VideoUploadException.class)
        public ResponseEntity<Map<String, Object>> handleVideoUploadException(VideoUploadException ex) {
                log.error("Video upload error: {} (contentId={}, videoAssetId={}, uploadId={}, phase={})",
                                ex.getMessage(), ex.getContentId(), ex.getVideoAssetId(),
                                ex.getUploadId(), ex.getPhase());
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "Video upload failed");
                body.put("message", ex.getMessage() != null ? ex.getMessage() : "Unknown error");
                body.put("phase", ex.getPhase() != null ? ex.getPhase().name() : "UNKNOWN");
                if (ex.getContentId() != null) {
                        body.put("contentId", ex.getContentId().toString());
                }
                if (ex.getVideoAssetId() != null) {
                        body.put("videoAssetId", ex.getVideoAssetId().toString());
                }
                if (ex.getUploadId() != null) {
                        body.put("uploadId", ex.getUploadId());
                }
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(withCorrelation(body));
        }
}
