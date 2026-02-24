package com.streamflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for sprite sheet metadata (timeline hover preview). Supports
 * multiple sprite sheets per video; client loads the sheet that contains the
 * hover frame (sheetIndex = frameNumber / framesPerSheet).
 */
@Data
@Builder
public class SpriteSheetResponse {

    private UUID contentId;
    private UUID videoAssetId;

    /** Common layout: interval and dimensions apply to all sheets. */
    private Integer intervalSeconds;
    private Integer thumbWidth;
    private Integer thumbHeight;
    private Integer columnsCount;

    /** Frames per sheet (e.g. 100); used to compute which sheet to load. */
    private Integer framesPerSheet;

    /** Ordered list of sprite sheets (by sheetIndex). */
    private List<SpriteSheetEntry> sheets;
}
