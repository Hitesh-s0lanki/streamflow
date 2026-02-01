package com.streamflow.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VideoAssetResponse {

    private UUID id;
    private UUID contentId;
    private UUID episodeId;
    private Integer durationSeconds;
    private String manifestUrl;
    private Boolean drmEnabled;
    private Instant createdAt;
}
