package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-frame coordinates and time offset for a sprite sheet (advanced preview
 * mapping).
 */
@Getter
@Setter
@Entity
@Table(name = "sprite_frame_metadata", indexes = {
        @Index(name = "idx_sprite_frame_sprite_sheet_id", columnList = "sprite_sheet_id"),
        @Index(name = "idx_sprite_frame_time", columnList = "sprite_sheet_id, time_offset_seconds")
})
public class SpriteFrameMetadata extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprite_sheet_id", nullable = false)
    private SpriteSheet spriteSheet;

    @Column(name = "frame_index", nullable = false)
    private Integer frameIndex;

    @Column(name = "time_offset_seconds", nullable = false)
    private Integer timeOffsetSeconds;

    @Column(name = "x_position")
    private Integer xPosition;

    @Column(name = "y_position")
    private Integer yPosition;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;
}
