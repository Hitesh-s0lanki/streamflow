package com.streamflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/video-assets/{videoAssetId}/sprites.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSpriteRequest {

    @NotBlank(message = "spriteUrl is required")
    private String spriteUrl;

    @NotNull(message = "startTimeSeconds is required")
    private Integer startTimeSeconds;

    @NotNull(message = "endTimeSeconds is required")
    private Integer endTimeSeconds;

    @NotNull(message = "columns is required")
    private Integer columns;

    @NotNull(message = "rows is required")
    private Integer rows;

    @NotNull(message = "thumbnailWidth is required")
    private Integer thumbnailWidth;

    @NotNull(message = "thumbnailHeight is required")
    private Integer thumbnailHeight;

    /** Optional; used for seek preview mapping. */
    private Integer intervalSeconds;
}
