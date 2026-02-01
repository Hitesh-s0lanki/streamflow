package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Sprite sheet metadata for playback (seek preview).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpriteSheetResponse {

    private UUID id;
    private String spriteUrl;
    private Integer startTimeSeconds;
    private Integer endTimeSeconds;
    private Integer columns;
    private Integer rows;
    private Integer thumbnailWidth;
    private Integer thumbnailHeight;
    private Integer intervalSeconds;
}
