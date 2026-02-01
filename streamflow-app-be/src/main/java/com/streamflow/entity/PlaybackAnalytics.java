package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Aggregated playback metrics derived from Kafka consumers (optional).
 */
@Getter
@Setter
@Entity
@Table(name = "playback_analytics", indexes = {
        @Index(name = "idx_playback_analytics_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_playback_analytics_period", columnList = "video_asset_id, period_start")
})
public class PlaybackAnalytics extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "period_start", nullable = false)
    private java.time.Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private java.time.Instant periodEnd;

    @Column(name = "total_plays")
    private Long totalPlays;

    @Column(name = "unique_viewers")
    private Long uniqueViewers;

    @Column(name = "avg_watch_time_seconds")
    private Integer avgWatchTimeSeconds;

    @Column(name = "completion_rate", precision = 5, scale = 4)
    private BigDecimal completionRate;

    @Column(name = "buffering_rate", precision = 5, scale = 4)
    private BigDecimal bufferingRate;
}
