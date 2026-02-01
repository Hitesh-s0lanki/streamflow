package com.streamflow.entity;

import com.streamflow.entity.enums.PlaybackEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Sampled playback events for debugging; full stream goes to Kafka.
 */
@Getter
@Setter
@Entity
@Table(name = "playback_event_log", indexes = {
        @Index(name = "idx_playback_event_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_playback_event_user_id", columnList = "user_id"),
        @Index(name = "idx_playback_event_created_at", columnList = "created_at"),
        @Index(name = "idx_playback_event_type", columnList = "event_type")
})
public class PlaybackEventLog extends BaseEntity {

    @Column(name = "user_id", length = 256)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_asset_id")
    private VideoAsset videoAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private PlaybackEventType eventType;

    @Column(name = "current_time_seconds")
    private Integer currentTimeSeconds;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;
}
