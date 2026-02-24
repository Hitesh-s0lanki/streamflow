package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Sprite sheet metadata for a video asset. A video can have multiple sprite
 * sheets (one DB row per sheet). Each sheet contains a fixed number of frames
 * (e.g. 100). The frontend loads only the sheet that contains the hover frame
 * for efficient preview (sheetIndex = frameNumber / framesPerSheet).
 */
@Getter
@Setter
@Entity
@Table(name = "sprite_sheet", indexes = {
        @Index(name = "idx_sprite_sheet_video_asset_sheet", columnList = "video_asset_id, sheet_index")
})
public class SpriteSheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    /** 0-based order of this sprite sheet for the video. */
    @Column(name = "sheet_index", nullable = false)
    private Integer sheetIndex;

    /** First frame number (0-based) included in this sheet. */
    @Column(name = "start_frame", nullable = false)
    private Integer startFrame;

    /** Last frame number (0-based, inclusive) included in this sheet. */
    @Column(name = "end_frame", nullable = false)
    private Integer endFrame;

    /**
     * End time in seconds for this sheet (last frame's timestamp).
     * Legacy DB column; derived as endFrame * intervalSeconds.
     */
    @Column(name = "end_time_seconds", nullable = false)
    private Integer endTimeSeconds = 0;

    /** Number of frames in this sheet (endFrame - startFrame + 1). */
    @Column(name = "frames_count", nullable = false)
    private Integer framesCount;

    /** S3 key of the sprite image (e.g. sprites/abc-123-0.jpg). */
    @Column(name = "sprite_s3_key", nullable = false, length = 1024)
    private String spriteS3Key;

    /** Interval in seconds between consecutive thumbnails (e.g. 10). */
    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds;

    /** Width of each thumbnail in the sprite (e.g. 160). */
    @Column(name = "thumb_width", nullable = false)
    private Integer thumbWidth;

    /** Height of each thumbnail in the sprite (e.g. 90). */
    @Column(name = "thumb_height", nullable = false)
    private Integer thumbHeight;

    /** Number of columns in the sprite grid. */
    @Column(name = "columns_count", nullable = false)
    private Integer columnsCount;

    @Column(name = "columns", nullable = false)
    private Integer columns = 10;

    /** Keep legacy "columns" in sync with columnsCount. */
    public void setColumnsCount(Integer columnsCount) {
        this.columnsCount = columnsCount;
        this.columns = columnsCount != null ? columnsCount : 10;
    }

    /** Keep endTimeSeconds in sync with endFrame and intervalSeconds. */
    public void setEndFrame(Integer endFrame) {
        this.endFrame = endFrame;
        this.endTimeSeconds = (endFrame != null && intervalSeconds != null)
                ? endFrame * intervalSeconds
                : 0;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        this.endTimeSeconds = (endFrame != null && intervalSeconds != null)
                ? endFrame * intervalSeconds
                : (this.endTimeSeconds != null ? this.endTimeSeconds : 0);
    }

    /** Number of rows in the sprite grid. */
    @Column(name = "rows_count", nullable = false)
    private Integer rowsCount;

    @Column(name = "rows", nullable = false)
    private Integer rows = 0;

    /** Keep legacy "rows" in sync with rowsCount. */
    public void setRowsCount(Integer rowsCount) {
        this.rowsCount = rowsCount;
        this.rows = rowsCount != null ? rowsCount : 0;
    }
}
