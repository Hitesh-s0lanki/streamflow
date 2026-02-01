package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * A single episode of a series. Each episode has one playable VideoAsset.
 */
@Getter
@Setter
@Entity
@Table(name = "episode", indexes = {
        @Index(name = "idx_episode_season_id", columnList = "season_id"),
        @Index(name = "idx_episode_video_asset_id", columnList = "video_asset_id", unique = true),
        @Index(name = "idx_episode_season_number", columnList = "season_id, episode_number", unique = true)
})
public class Episode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private SeriesSeason season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Duration in seconds. */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false, unique = true)
    private VideoAsset videoAsset;
}
