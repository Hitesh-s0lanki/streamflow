package com.streamflow.dto.admin;

import com.streamflow.entity.enums.IngestionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class IngestionJobResponse {

    private UUID id;
    private UUID videoAssetId;
    private IngestionStatus jobStatus;
    private String rawS3Key;
    private String errorMessage;
    private Instant processedAt;
    private Instant createdAt;
}
