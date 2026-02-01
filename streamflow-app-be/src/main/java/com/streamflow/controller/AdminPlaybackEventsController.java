package com.streamflow.controller;

import com.streamflow.dto.PagedResponse;
import com.streamflow.dto.PlaybackEventLogItemResponse;
import com.streamflow.entity.enums.PlaybackEventType;
import com.streamflow.service.PlaybackService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin: sampled playback events for debug. Order: newest first.
 */
@RestController
@RequestMapping("/api/admin/playback-events")
public class AdminPlaybackEventsController {

    private final PlaybackService playbackService;

    public AdminPlaybackEventsController(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    /**
     * List playback events with filters. Order: newest first. Paginated (limit = size).
     */
    @GetMapping
    public ResponseEntity<PagedResponse<PlaybackEventLogItemResponse>> listPlaybackEvents(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) UUID videoAssetId,
            @RequestParam(required = false) PlaybackEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PagedResponse<PlaybackEventLogItemResponse> result = playbackService.adminListPlaybackEvents(
                userId, videoAssetId, eventType, from, to, page, size);
        return ResponseEntity.ok(result);
    }
}
