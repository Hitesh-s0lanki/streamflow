package com.streamflow.worker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Kafka payload for media.ingestion.requested / streamflow.ingestion.jobs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionEventPayload {

    private UUID jobId;
    private UUID videoAssetId;
    private String rawS3Key;
    private String contentType;
    private boolean drmEnabled;

    public String getContentType() {
        return contentType != null && !contentType.isBlank() ? contentType : "video/mp4";
    }
}
