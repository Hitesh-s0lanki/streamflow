package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request body for POST /api/admin/analytics/rebuild.
 */
public record RebuildAnalyticsRequest(
                @NotNull Instant from,
                @NotNull Instant to) {
}
