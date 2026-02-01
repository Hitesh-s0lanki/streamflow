package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * A season within a Series. Only used when Content.contentType = SERIES.
 */
@Getter
@Setter
@Entity
@Table(name = "series_season", indexes = {
        @Index(name = "idx_series_season_content_id", columnList = "content_id"),
        @Index(name = "idx_series_season_number", columnList = "content_id, season_number", unique = true)
})
public class SeriesSeason extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_id", nullable = false)
    private Content content;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "poster_url", length = 1024)
    private String posterUrl;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("episodeNumber ASC")
    private List<Episode> episodes = new ArrayList<>();
}
