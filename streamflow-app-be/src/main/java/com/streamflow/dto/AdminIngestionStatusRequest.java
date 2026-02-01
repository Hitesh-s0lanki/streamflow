package com.streamflow.dto;

import com.streamflow.entity.enums.IngestionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/admin/ingestion/{jobId}/status.
 */
@Getter
@Setter
public class AdminIngestionStatusRequest {

    @NotNull(message = "jobStatus is required")
    private IngestionStatus jobStatus;
}
