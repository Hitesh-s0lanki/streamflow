package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Season summary for content detail and GET /api/content/{id}/seasons.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonSummaryResponse {

    private UUID id;
    private Integer seasonNumber;
    private String title;
    private String posterUrl;
}
