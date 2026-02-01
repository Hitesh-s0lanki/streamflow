package com.streamflow.controller;

import com.streamflow.dto.IngestionStatusResponse;
import com.streamflow.dto.PagedResponse;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.service.IngestionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin: list ingestion jobs and get job detail. GET /api/admin/ingestion-jobs.
 */
@RestController
@RequestMapping("/api/admin/ingestion-jobs")
public class AdminIngestionJobsController {

    private final IngestionService ingestionService;

    public AdminIngestionJobsController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * List ingestion jobs with filters. Order: createdAt DESC. Paginated.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<IngestionStatusResponse>> listJobs(
            @RequestParam(required = false) IngestionStatus jobStatus,
            @RequestParam(required = false) UUID videoAssetId,
            @RequestParam(required = false) UUID contentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<IngestionStatusResponse> result = ingestionService.adminListJobs(
                jobStatus, videoAssetId, contentId, from, to, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Get ingestion job detail (rawS3Key, errorMessage, processedAt).
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<IngestionStatusResponse> getJobDetail(@PathVariable UUID jobId) {
        IngestionStatusResponse detail = ingestionService.adminGetJobDetail(jobId);
        return ResponseEntity.ok(detail);
    }
}
