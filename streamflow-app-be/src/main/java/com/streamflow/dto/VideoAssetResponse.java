package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Response for POST /api/video-assets (created VideoAsset).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoAssetResponse {

    private UUID id;
    private UUID contentId;
    private UUID episodeId;
    private Integer durationSeconds;
    private Boolean drmEnabled;
}
