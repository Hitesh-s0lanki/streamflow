package com.streamflow.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin analytics overview: same as public overview plus ingestion readiness
 * count.
 */
public record AdminAnalyticsOverviewResponse(
                long totalVideos,
                long totalPlays,
                long totalUniqueViewers,
                BigDecimal avgCompletionRate,
                BigDecimal avgBufferingRate,
                List<AnalyticsOverviewResponse.TopVideoSummary> topVideos,
                long ingestionReadyCount) {
}
