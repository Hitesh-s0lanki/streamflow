package com.streamflow.entity;

import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Root catalog entity: a Movie or Series visible in the UI (home, search,
 * preview).
 */
@Getter
@Setter
@Entity
@Table(name = "content", indexes = {
        @Index(name = "idx_content_content_type", columnList = "content_type"),
        @Index(name = "idx_content_publish_status", columnList = "publish_status"),
        @Index(name = "idx_content_release_year", columnList = "release_year")
})
public class Content extends BaseEntity {

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 32)
    private ContentType contentType;

    @Column(name = "release_year")
    private Integer releaseYear;

    @Column(name = "rating", length = 16)
    private String rating;

    @Column(name = "poster_url", length = 1024)
    private String posterUrl;

    @Column(name = "thumbnail_url", length = 1024)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_status", nullable = false, length = 32)
    private PublishStatus publishStatus = PublishStatus.DRAFT;

    /** Duration in seconds; used for MOVIE. For SERIES, derived from episodes. */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("seasonNumber ASC")
    private List<SeriesSeason> seasons = new ArrayList<>();

    /**
     * For MOVIE: single video asset. For SERIES: no direct link; use Episode →
     * VideoAsset.
     */
    @OneToOne(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private VideoAsset videoAsset;
}
