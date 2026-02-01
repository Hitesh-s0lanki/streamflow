package com.streamflow.controller;

import com.streamflow.dto.ContinueWatchingItemResponse;
import com.streamflow.dto.UpsertWatchProgressRequest;
import com.streamflow.dto.WatchProgressResponse;
import com.streamflow.service.WatchProgressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Watch progress and resume: upsert position, get progress, mark completed,
 * continue watching row.
 * userId is derived from X-User-Id (Clerk integration).
 */
@RestController
@RequestMapping("/api/watch-progress")
public class WatchProgressController {

    private static final String HEADER_USER_ID = "X-User-Id";

    private final WatchProgressService watchProgressService;

    public WatchProgressController(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    /**
     * Upsert watch progress. Save current playback position; safe to call
     * frequently.
     */
    @PostMapping
    public ResponseEntity<WatchProgressResponse> upsert(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @Valid @RequestBody UpsertWatchProgressRequest request) {
        WatchProgressResponse response = watchProgressService.upsert(
                userId,
                request.getVideoAssetId(),
                request.getLastWatchedSecond(),
                request.getCompleted());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Get watch progress for a video asset. Returns default (lastWatchedSecond=0,
     * completed=false) if no record.
     */
    @GetMapping("/{videoAssetId}")
    public ResponseEntity<WatchProgressResponse> getByVideoAsset(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @PathVariable UUID videoAssetId) {
        WatchProgressResponse response = watchProgressService.getByVideoAsset(userId, videoAssetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Mark video as completed (e.g. from UI). Upserts if missing.
     */
    @PostMapping("/{videoAssetId}/complete")
    public ResponseEntity<WatchProgressResponse> markComplete(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @PathVariable UUID videoAssetId) {
        WatchProgressResponse response = watchProgressService.markComplete(userId, videoAssetId);
        return ResponseEntity.ok(response);
    }

    /**
     * Continue watching row: in-progress only, last 30 days, ordered by
     * lastWatchedAt DESC.
     */
    @GetMapping("/continue")
    public ResponseEntity<List<ContinueWatchingItemResponse>> getContinueWatching(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId) {
        List<ContinueWatchingItemResponse> list = watchProgressService.getContinueWatching(userId);
        return ResponseEntity.ok(list);
    }
}
