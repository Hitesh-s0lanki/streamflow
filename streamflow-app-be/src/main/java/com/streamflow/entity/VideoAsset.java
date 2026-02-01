package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A single playable video unit. Movie → 1 VideoAsset; Episode → 1 VideoAsset.
 */
@Getter
@Setter
@Entity
@Table(name = "video_asset", indexes = {
        @Index(name = "idx_video_asset_content_id", columnList = "content_id"),
        @Index(name = "idx_video_asset_episode_id", columnList = "episode_id", unique = true)
})
public class VideoAsset extends BaseEntity {

    /**
     * For MOVIE: the content. For SERIES: the series (content) this asset belongs
     * to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private Content content;

    /** For SERIES episode: the episode. Null for movie. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", unique = true)
    private Episode episode;

    /** Duration in seconds. */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Column(name = "manifest_url", length = 1024)
    private String manifestUrl;

    @Column(name = "drm_enabled", nullable = false)
    private Boolean drmEnabled = false;

    @OneToMany(mappedBy = "videoAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "videoAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SpriteSheet> spriteSheets = new ArrayList<>();

    @OneToMany(mappedBy = "videoAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngestionJob> ingestionJobs = new ArrayList<>();
}
