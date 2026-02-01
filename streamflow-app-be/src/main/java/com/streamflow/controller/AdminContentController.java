package com.streamflow.controller;

import com.streamflow.dto.*;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin content: list all (including DRAFT), update metadata, unpublish.
 * All under /api/admin/content.
 */
@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private final ContentService contentService;

    public AdminContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /**
     * List all content with optional filters. Order: createdAt DESC. Paginated.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<AdminContentListItemResponse>> listContent(
            @RequestParam(required = false) PublishStatus publishStatus,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<AdminContentListItemResponse> result = contentService.adminListContent(
                publishStatus, contentType, title, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * Update content metadata (title, description, releaseYear, rating, posterUrl, thumbnailUrl).
     * contentType must not be changed.
     */
    @PatchMapping("/{contentId}")
    public ResponseEntity<ContentDetailResponse> updateMetadata(
            @PathVariable UUID contentId,
            @Valid @RequestBody UpdateContentMetadataRequest request) {
        ContentDetailResponse updated = contentService.updateContentMetadata(contentId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Unpublish content (PUBLISHED → DRAFT). Instantly removed from public catalog.
     */
    @PostMapping("/{contentId}/unpublish")
    public ResponseEntity<ContentDetailResponse> unpublish(@PathVariable UUID contentId) {
        ContentDetailResponse updated = contentService.unpublishContent(contentId);
        return ResponseEntity.ok(updated);
    }
}
