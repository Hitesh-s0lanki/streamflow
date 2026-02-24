package com.streamflow.dto;

import com.streamflow.entity.enums.ProcessingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class VideoProcessingResponse {

    private UUID contentId;
    private UUID videoAssetId;
    private ProcessingStatus processingStatus;
    private Instant processingStartedAt;
    private Instant processingCompletedAt;
    private String errorMessage;
    private String message;
}
