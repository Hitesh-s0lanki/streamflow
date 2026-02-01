package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/seasons/{seasonId}/episodes.
 */
@Getter
@Setter
public class CreateEpisodeRequest {

    @NotNull(message = "episodeNumber is required")
    private Integer episodeNumber;

    @NotNull(message = "title is required")
    @Size(max = 512)
    private String title;

    @NotNull(message = "durationSeconds is required")
    private Integer durationSeconds;

    @Size(max = 65535)
    private String description;

    @Size(max = 1024)
    private String thumbnailUrl;
}
