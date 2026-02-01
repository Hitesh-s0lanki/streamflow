package com.streamflow.controller;

import com.streamflow.dto.AnalyticsOverviewResponse;
import com.streamflow.dto.VideoAnalyticsRecordResponse;
import com.streamflow.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Analytics read endpoints (Phase 7). Playback metrics for dashboards and
 * demos.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Get analytics for a video. Optional from/to filter; ordering periodStart
     * DESC.
     */
    @GetMapping("/video/{videoAssetId}")
    public ResponseEntity<List<VideoAnalyticsRecordResponse>> getVideoAnalytics(
            @PathVariable UUID videoAssetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String window) {
        List<VideoAnalyticsRecordResponse> list = analyticsService.getVideoAnalytics(videoAssetId, from, to);
        return ResponseEntity.ok(list);
    }

    /**
     * Platform analytics overview: totalVideos, totalPlays, totalUniqueViewers,
     * avgCompletionRate, avgBufferingRate, topVideos.
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        AnalyticsOverviewResponse overview = analyticsService.getOverview(from, to);
        return ResponseEntity.ok(overview);
    }
}
