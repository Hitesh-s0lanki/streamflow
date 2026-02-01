package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Episode fields for GET /api/seasons/{seasonId}/episodes.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeListItemResponse {

    private UUID id;
    private Integer episodeNumber;
    private String title;
    private Integer durationSeconds;
    private String thumbnailUrl;
}
