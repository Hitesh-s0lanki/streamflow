package com.streamflow.dto;

import com.streamflow.entity.enums.IngestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for GET /api/ingestion/{videoAssetId} and admin ingestion job
 * detail.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionStatusResponse {

    private UUID jobId;
    private UUID videoAssetId;
    private IngestionStatus jobStatus;
    private Instant processedAt;
    private String errorMessage;
    /** Admin detail only: raw S3 key (do not log in production). */
    private String rawS3Key;
    private java.time.Instant createdAt;
}
