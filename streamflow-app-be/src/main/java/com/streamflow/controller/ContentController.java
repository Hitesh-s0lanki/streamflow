package com.streamflow.controller;

import com.streamflow.dto.*;
import com.streamflow.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Phase 1 catalog API: Content, SeriesSeason, Episode.
 * Public read routes: GET catalog, GET detail, GET seasons, GET episodes.
 */
@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * Route 1: Create Movie or Series shell in DRAFT.
     * POST /api/content
     */
    @PostMapping
    public ResponseEntity<ContentDetailResponse> createContent(@Valid @RequestBody CreateContentRequest request) {
        ContentDetailResponse created = contentService.createContent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Route 2: Publish content (DRAFT → PUBLISHED).
     * POST /api/content/{contentId}/publish
     */
    @PostMapping("/{contentId}/publish")
    public ResponseEntity<ContentDetailResponse> publishContent(@PathVariable UUID contentId) {
        ContentDetailResponse updated = contentService.publishContent(contentId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Route 3: Catalog listing for home tiles (PUBLISHED only, createdAt DESC).
     * GET /api/content
     */
    @GetMapping
    public ResponseEntity<List<ContentCatalogItemResponse>> getCatalog() {
        List<ContentCatalogItemResponse> list = contentService.getCatalogListing();
        return ResponseEntity.ok(list);
    }

    /**
     * Route 4: Content detail (movie or series). PUBLISHED always; DRAFT via direct
     * ID.
     * GET /api/content/{contentId}
     */
    @GetMapping("/{contentId}")
    public ResponseEntity<ContentDetailResponse> getContentDetail(@PathVariable UUID contentId) {
        ContentDetailResponse detail = contentService.getContentDetail(contentId);
        return ResponseEntity.ok(detail);
    }

    /**
     * Route 5: Create season for a series.
     * POST /api/content/{contentId}/seasons
     */
    @PostMapping("/{contentId}/seasons")
    public ResponseEntity<SeasonSummaryResponse> createSeason(
            @PathVariable UUID contentId,
            @Valid @RequestBody CreateSeasonRequest request) {
        SeasonSummaryResponse created = contentService.createSeason(contentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Route 6: List seasons for a series (seasonNumber ASC).
     * GET /api/content/{contentId}/seasons
     */
    @GetMapping("/{contentId}/seasons")
    public ResponseEntity<List<SeasonSummaryResponse>> getSeasons(@PathVariable UUID contentId) {
        List<SeasonSummaryResponse> list = contentService.getSeasonsForContent(contentId);
        return ResponseEntity.ok(list);
    }
}
