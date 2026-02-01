package com.streamflow.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One analytics record for GET /api/analytics/video/{videoAssetId}.
 */
public record VideoAnalyticsRecordResponse(
                Instant periodStart,
                Instant periodEnd,
                Long totalPlays,
                Long uniqueViewers,
                Integer avgWatchTimeSeconds,
                BigDecimal completionRate,
                BigDecimal bufferingRate) {
}
