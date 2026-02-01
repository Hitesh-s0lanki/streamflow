package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for POST /api/playback/events (Phase 6). Emits to Kafka for
 * analytics.
 */
public record PlaybackEventRequest(
                @NotNull String eventType,
                @NotNull UUID videoAssetId,
                String userId,
                Integer currentTimeSeconds) {
}
