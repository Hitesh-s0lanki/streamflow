package com.streamflow.dto.admin;

import com.streamflow.entity.enums.IngestionStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TriggerIngestResponse {

    private UUID jobId;
    private UUID videoAssetId;
    private IngestionStatus status;
    private String rawS3Key;
}
