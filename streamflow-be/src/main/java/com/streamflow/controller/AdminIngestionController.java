package com.streamflow.controller;

import com.streamflow.dto.admin.*;
import com.streamflow.service.AdminIngestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin API for content creation, video asset creation, presigned upload URLs,
 * triggering ingestion, and checking ingestion job status. Connects UI →
 * Backend → AWS → Kafka.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminIngestionController {

    private final AdminIngestionService adminIngestionService;

    /**
     * A. Create Content (Movie or Series).
     */
    @PostMapping(value = "/content", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ContentResponse createContent(@Valid @RequestBody CreateContentRequest request) {
        return adminIngestionService.createContent(request);
    }

    /**
     * B. Create Video Asset placeholder linked to Movie or Series.
     */
    @PostMapping(value = "/video-assets", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public VideoAssetResponse createVideoAsset(@Valid @RequestBody CreateVideoAssetRequest request) {
        return adminIngestionService.createVideoAsset(request);
    }

    /**
     * C. Generate pre-signed upload URL for a video asset.
     */
    @PostMapping(value = "/video-assets/{videoAssetId}/upload-url", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PresignedUploadResponse getUploadUrl(
            @PathVariable UUID videoAssetId,
            @RequestBody(required = false) PresignedUploadRequest body) {
        String contentType = body != null && body.getContentType() != null ? body.getContentType()
                : "application/octet-stream";
        return adminIngestionService.generatePresignedUploadUrl(videoAssetId, contentType);
    }

    /**
     * D. Trigger ingestion: create ingestion job and publish Kafka event.
     */
    @PostMapping(value = "/video-assets/{videoAssetId}/ingest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TriggerIngestResponse triggerIngest(@PathVariable UUID videoAssetId) {
        return adminIngestionService.triggerIngest(videoAssetId);
    }

    /**
     * E. Check ingestion job status (for UI polling).
     */
    @GetMapping(value = "/ingestion-jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IngestionJobResponse getIngestionJob(@PathVariable UUID jobId) {
        return adminIngestionService.getIngestionJob(jobId);
    }
}
