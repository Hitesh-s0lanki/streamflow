package com.streamflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Single sprite sheet entry in a multi-sheet response. Player uses sheetIndex
 * and startFrame/endFrame to choose which sheet to load for a given hover time.
 */
@Data
@Builder
public class SpriteSheetEntry {

    private UUID spriteSheetId;
    /** URL to this sheet's sprite image. */
    private String spriteUrl;
    /** 0-based index of this sheet. */
    private Integer sheetIndex;
    private Integer startFrame;
    private Integer endFrame;
    private Integer framesCount;
    private Integer rowsCount;
}
