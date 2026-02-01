package com.streamflow.dto;

import com.streamflow.entity.enums.PlaybackEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin playback event log item (sampled events for debug).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackEventLogItemResponse {

    private UUID id;
    private String userId;
    private UUID videoAssetId;
    private PlaybackEventType eventType;
    private Integer currentTimeSeconds;
    private String payload;
    private Instant createdAt;
}
