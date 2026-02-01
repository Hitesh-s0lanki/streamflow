package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * One item in the Continue Watching row (GET /api/watch-progress/continue).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContinueWatchingItemResponse {

    private UUID videoAssetId;
    private UUID contentId;
    /** Present when the asset is for an episode. */
    private UUID episodeId;
    /** Content title; for series this is the show title. */
    private String title;
    /** Optional episode title for series. */
    private String episodeTitle;
    private String posterUrl;
    private String thumbnailUrl;
    private Integer lastWatchedSecond;
    private Integer durationSeconds;
    private Instant lastWatchedAt;
}
