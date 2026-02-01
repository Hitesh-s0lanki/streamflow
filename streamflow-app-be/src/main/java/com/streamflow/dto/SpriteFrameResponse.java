package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Response for sprite frame metadata (frame-accurate preview mapping).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpriteFrameResponse {

    private UUID id;
    private Integer frameIndex;
    private Integer timeOffsetSeconds;
    private Integer xPosition;
    private Integer yPosition;
    private Integer width;
    private Integer height;
}
