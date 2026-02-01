package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/video-assets.
 * For MOVIE: contentId must be set, episodeId null.
 * For SERIES: episodeId must be set; contentId is derived from episode.
 */
@Getter
@Setter
public class CreateVideoAssetRequest {

    @NotNull(message = "durationSeconds is required")
    @Positive(message = "durationSeconds must be positive")
    private Integer durationSeconds;

    /** For MOVIE: the content (movie) id. Must be null when episodeId is set. */
    private UUID contentId;

    /** For SERIES: the episode id. Must be null when contentId is set. */
    private UUID episodeId;
}
