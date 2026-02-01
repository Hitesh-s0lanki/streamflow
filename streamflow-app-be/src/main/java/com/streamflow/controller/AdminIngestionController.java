package com.streamflow.controller;

import com.streamflow.dto.AdminIngestionStatusRequest;
import com.streamflow.dto.IngestionStatusResponse;
import com.streamflow.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin-only: override ingestion job status for demo and manual correction.
 * POST /api/admin/ingestion/{jobId}/status — set jobStatus (allowed transitions
 * only).
 */
@RestController
@RequestMapping("/api/admin/ingestion")
public class AdminIngestionController {

    private final IngestionService ingestionService;

    public AdminIngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/{jobId}/status")
    public ResponseEntity<IngestionStatusResponse> overrideStatus(
            @PathVariable UUID jobId,
            @Valid @RequestBody AdminIngestionStatusRequest request) {
        IngestionStatusResponse response = ingestionService.adminOverrideStatus(jobId, request.getJobStatus());
        return ResponseEntity.ok(response);
    }
}
