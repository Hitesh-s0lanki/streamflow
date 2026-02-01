package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for GET /api/watch-progress/{videoAssetId}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchProgressResponse {

    private UUID videoAssetId;
    private Integer lastWatchedSecond;
    private Boolean completed;
    private Instant lastWatchedAt;
}
