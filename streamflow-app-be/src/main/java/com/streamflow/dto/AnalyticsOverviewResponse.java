package com.streamflow.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Platform analytics overview for GET /api/analytics/overview.
 */
public record AnalyticsOverviewResponse(
        long totalVideos,
        long totalPlays,
        long totalUniqueViewers,
        BigDecimal avgCompletionRate,
        BigDecimal avgBufferingRate,
        List<TopVideoSummary> topVideos) {
    public record TopVideoSummary(java.util.UUID videoAssetId, long totalPlays) {
    }
}
