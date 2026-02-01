package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/sprites/{spriteSheetId}/frames.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterSpriteFrameRequest {

    @NotNull(message = "frameIndex is required")
    private Integer frameIndex;

    @NotNull(message = "timeOffsetSeconds is required")
    private Integer timeOffsetSeconds;

    private Integer xPosition;
    private Integer yPosition;
    private Integer width;
    private Integer height;
}
