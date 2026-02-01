package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full content for GET /api/content/{id}. For SERIES, includes seasons (ordered
 * by seasonNumber).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private ContentType contentType;
    private Integer releaseYear;
    private String rating;
    private String posterUrl;
    private String thumbnailUrl;
    private PublishStatus publishStatus;
    private Integer durationSeconds;
    private Instant createdAt;
    private Instant updatedAt;

    /** For SERIES only; ordered by seasonNumber ASC. Empty for MOVIE. */
    private List<SeasonSummaryResponse> seasons;
}
