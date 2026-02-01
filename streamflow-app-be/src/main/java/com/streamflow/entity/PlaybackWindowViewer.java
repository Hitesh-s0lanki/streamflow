package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks distinct viewers per (videoAsset, period) for uniqueViewers
 * aggregation.
 * One row per userId that had a PLAY event in the window; used to count
 * uniqueViewers.
 */
@Getter
@Setter
@Entity
@Table(name = "playback_window_viewer", indexes = {
        @Index(name = "idx_playback_window_viewer_video_period", columnList = "video_asset_id, period_start"),
        @Index(name = "uk_playback_window_viewer_video_period_user", columnList = "video_asset_id, period_start, user_id", unique = true)
})
public class PlaybackWindowViewer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "user_id", nullable = false, length = 256)
    private String userId;
}
