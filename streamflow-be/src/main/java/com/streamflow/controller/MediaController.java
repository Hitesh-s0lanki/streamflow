package com.streamflow.controller;

import com.streamflow.dto.PlaybackSessionRequest;
import com.streamflow.dto.PlaybackSessionResponse;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.service.PlaybackService;
import com.streamflow.service.S3StorageService;
import com.streamflow.service.S3StorageService.PresignedGetUrlResult;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final S3StorageService s3StorageService;
    private final PlaybackService playbackService;

    private static final int DEFAULT_EXPIRATION_MINUTES = 60;

    /**
     * Generic pre-signed GET URL for any S3 key (images, sprites, etc.).
     * Prefer the playback session endpoint for streaming use cases.
     */
    @GetMapping("/url")
    public ResponseEntity<?> getPresignedUrl(
            @RequestParam("key") String key,
            @RequestParam(value = "expirationMinutes", required = false) Integer expirationMinutes) {
        try {
            int expiry = (expirationMinutes != null && expirationMinutes > 0)
                    ? expirationMinutes
                    : DEFAULT_EXPIRATION_MINUTES;

            PresignedGetUrlResult result = s3StorageService.generatePresignedGetUrl(key, expiry);

            return ResponseEntity.ok(Map.of(
                    "url", result.getUrl(),
                    "expiresAt", result.getExpiresAt().toString(),
                    "key", key));
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for key={}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error",
                            "message", "Failed to generate URL for the requested key"));
        }
    }

    /**
     * Creates a playback session with pre-signed HLS manifest URL and
     * sprite sheet URLs. The frontend player uses this single response
     * to start adaptive streaming directly from S3 — no video bytes
     * pass through this server.
     */
    @PostMapping("/playback/sessions")
    public ResponseEntity<PlaybackSessionResponse> createPlaybackSession(
            @Valid @RequestBody PlaybackSessionRequest request) {
        PlaybackSessionResponse session = playbackService.createSession(request.getContentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Proxies HLS manifest, variant playlists, and segments from S3 so that the
     * player always requests from this API. Fixes 403 on variant/segment URLs
     * that would otherwise be resolved without the presigned query string.
     * Use when streamflow.api.base-url is set.
     */
    @GetMapping("/playback/stream/{contentId}/{*path}")
    public ResponseEntity<StreamingResponseBody> streamHls(
            @PathVariable UUID contentId,
            @PathVariable(required = false) String path) {
        String segment = (path != null && path.startsWith("/")) ? path.substring(1) : (path != null ? path : "");
        if (segment.isEmpty() || segment.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        String prefix = playbackService.getManifestKeyPrefix(contentId);
        String key = prefix + segment;
        try {
            ResponseInputStream<GetObjectResponse> s3Stream = s3StorageService.getObjectStream(key);
            GetObjectResponse resp = s3Stream.response();
            HttpHeaders headers = new HttpHeaders();
            if (resp.contentType() != null) {
                headers.setContentType(MediaType.parseMediaType(resp.contentType()));
            }
            if (resp.contentLength() != null && resp.contentLength() > 0) {
                headers.setContentLength(resp.contentLength());
            }
            StreamingResponseBody body = out -> {
                try (InputStream in = s3Stream) {
                    in.transferTo(out);
                }
            };
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(body);
        } catch (ResourceNotFoundException | com.streamflow.exception.BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to stream HLS key={} contentId={}", key, contentId, e);
            return ResponseEntity.notFound().build();
        }
    }
}
