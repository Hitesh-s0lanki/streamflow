package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One sprite image containing many preview frames for Netflix-style seek
 * preview.
 */
@Getter
@Setter
@Entity
@Table(name = "sprite_sheet", indexes = {
        @Index(name = "idx_sprite_sheet_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_sprite_sheet_time_range", columnList = "video_asset_id, start_time_seconds, end_time_seconds")
})
public class SpriteSheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "sprite_url", nullable = false, length = 1024)
    private String spriteUrl;

    @Column(name = "start_time_seconds", nullable = false)
    private Integer startTimeSeconds;

    @Column(name = "end_time_seconds", nullable = false)
    private Integer endTimeSeconds;

    @Column(name = "columns", nullable = false)
    private Integer columns;

    @Column(name = "rows", nullable = false)
    private Integer rows;

    @Column(name = "thumbnail_width", nullable = false)
    private Integer thumbnailWidth;

    @Column(name = "thumbnail_height", nullable = false)
    private Integer thumbnailHeight;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @OneToMany(mappedBy = "spriteSheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpriteFrameMetadata> frameMetadata = new ArrayList<>();
}
