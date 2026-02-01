package com.streamflow.controller;

import com.streamflow.dto.*;
import com.streamflow.service.VideoAssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * VideoAsset creation, presigned upload URL, and Phase 3: variants, manifest,
 * sprites.
 * POST /api/video-assets — create VideoAsset (movie or episode).
 * POST /api/video-assets/{id}/upload-url — get presigned PUT URL and raw S3
 * key.
 * POST /api/video-assets/{id}/variants — register ABR variant.
 * GET /api/video-assets/{id}/variants — list variants (ordered by sortOrder).
 * POST /api/video-assets/{id}/manifest — set DASH/HLS manifest URL.
 * POST /api/video-assets/{id}/sprites — register sprite sheet.
 * GET /api/video-assets/{id}/sprites — get sprite metadata for playback (seek
 * preview).
 */
@RestController
@RequestMapping("/api/video-assets")
public class VideoAssetController {

    private final VideoAssetService videoAssetService;

    public VideoAssetController(VideoAssetService videoAssetService) {
        this.videoAssetService = videoAssetService;
    }

    @PostMapping
    public ResponseEntity<VideoAssetResponse> createVideoAsset(@Valid @RequestBody CreateVideoAssetRequest request) {
        VideoAssetResponse created = videoAssetService.createVideoAsset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{videoAssetId}/upload-url")
    public ResponseEntity<UploadUrlResponse> getUploadUrl(@PathVariable UUID videoAssetId) {
        UploadUrlResponse response = videoAssetService.getUploadUrl(videoAssetId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{videoAssetId}/variants")
    public ResponseEntity<VariantResponse> addVariant(@PathVariable UUID videoAssetId,
            @Valid @RequestBody RegisterVariantRequest request) {
        VariantResponse created = videoAssetService.addVariant(videoAssetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{videoAssetId}/variants")
    public ResponseEntity<List<VariantResponse>> getVariants(@PathVariable UUID videoAssetId) {
        return ResponseEntity.ok(videoAssetService.getVariants(videoAssetId));
    }

    @PostMapping("/{videoAssetId}/manifest")
    public ResponseEntity<Void> setManifest(@PathVariable UUID videoAssetId,
            @Valid @RequestBody RegisterManifestRequest request) {
        videoAssetService.setManifest(videoAssetId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{videoAssetId}/sprites")
    public ResponseEntity<SpriteSheetResponse> addSpriteSheet(@PathVariable UUID videoAssetId,
            @Valid @RequestBody RegisterSpriteRequest request) {
        SpriteSheetResponse created = videoAssetService.addSpriteSheet(videoAssetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{videoAssetId}/sprites")
    public ResponseEntity<List<SpriteSheetResponse>> getSpritesForPlayback(@PathVariable UUID videoAssetId) {
        return ResponseEntity.ok(videoAssetService.getSpritesForPlayback(videoAssetId));
    }
}
