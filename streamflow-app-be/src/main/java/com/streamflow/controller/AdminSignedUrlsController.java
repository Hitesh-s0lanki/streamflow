package com.streamflow.controller;

import com.streamflow.dto.PagedResponse;
import com.streamflow.dto.SignedUrlAuditItemResponse;
import com.streamflow.service.PlaybackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin: signed URL audit trail (debug only). Metadata only, no active signed URLs.
 */
@RestController
@RequestMapping("/api/admin/signed-urls")
public class AdminSignedUrlsController {

    private final PlaybackService playbackService;

    public AdminSignedUrlsController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    /**
     * List signed URL records with filters. Order: createdAt DESC. Paginated. Metadata only.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<SignedUrlAuditItemResponse>> listSignedUrls(
            @RequestParam(required = false) UUID videoAssetId,
            @RequestParam(required = false) String urlType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<SignedUrlAuditItemResponse> result = playbackService.adminListSignedUrls(
                videoAssetId, urlType, from, to, page, size);
        return ResponseEntity.ok(result);
    }
}
