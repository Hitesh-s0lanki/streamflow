package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/content/{contentId}/seasons.
 */
@Getter
@Setter
public class CreateSeasonRequest {

    @NotNull(message = "seasonNumber is required")
    private Integer seasonNumber;

    @Size(max = 512)
    private String title;

    @Size(max = 1024)
    private String posterUrl;
}
