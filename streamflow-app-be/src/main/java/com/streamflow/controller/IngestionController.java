package com.streamflow.controller;

import com.streamflow.dto.ConfirmUploadRequest;
import com.streamflow.dto.IngestionStatusResponse;
import com.streamflow.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Ingestion lifecycle: confirm upload and get status.
 * POST /api/ingestion/{videoAssetId}/uploaded — confirm upload, create job,
 * emit Kafka.
 * GET /api/ingestion/{videoAssetId} — get current ingestion status.
 */
@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/{videoAssetId}/uploaded")
    public ResponseEntity<IngestionStatusResponse> confirmUpload(
            @PathVariable UUID videoAssetId,
            @Valid @RequestBody ConfirmUploadRequest request) {
        IngestionStatusResponse response = ingestionService.confirmUpload(videoAssetId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{videoAssetId}")
    public ResponseEntity<IngestionStatusResponse> getIngestionStatus(@PathVariable UUID videoAssetId) {
        IngestionStatusResponse response = ingestionService.getIngestionStatus(videoAssetId);
        return ResponseEntity.ok(response);
    }
}
