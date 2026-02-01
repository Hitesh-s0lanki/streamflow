package com.streamflow.controller;

import com.streamflow.dto.AdminAnalyticsOverviewResponse;
import com.streamflow.dto.RebuildAnalyticsRequest;
import com.streamflow.service.AnalyticsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * Admin: analytics overview (with ingestion readiness), rebuild. All under /api/admin/analytics.
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Analytics overview for admin: same as public overview plus ingestion readiness count.
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminAnalyticsOverviewResponse> getOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        AdminAnalyticsOverviewResponse overview = analyticsService.getAdminOverview(from, to);
        return ResponseEntity.ok(overview);
    }

    /**
     * Trigger analytics rebuild for a period range. Idempotent.
     */
    @PostMapping("/rebuild")
    public ResponseEntity<Void> rebuild(@Valid @RequestBody RebuildAnalyticsRequest request) {
        analyticsService.rebuildAnalytics(request.from(), request.to());
        return ResponseEntity.noContent().build();
    }
}
