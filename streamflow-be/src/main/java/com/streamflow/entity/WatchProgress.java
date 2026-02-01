package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Resume position per user (Clerk userId) and video asset.
 */
@Getter
@Setter
@Entity
@Table(name = "watch_progress", indexes = {
        @Index(name = "idx_watch_progress_user_id", columnList = "user_id"),
        @Index(name = "idx_watch_progress_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_watch_progress_user_asset", columnList = "user_id, video_asset_id", unique = true)
})
public class WatchProgress extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 256)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "last_watched_second", nullable = false)
    private Integer lastWatchedSecond = 0;

    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Column(name = "last_watched_at")
    private java.time.Instant lastWatchedAt;
}
