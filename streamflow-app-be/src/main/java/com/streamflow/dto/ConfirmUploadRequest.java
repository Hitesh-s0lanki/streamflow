package com.streamflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/ingestion/{videoAssetId}/uploaded.
 */
@Getter
@Setter
public class ConfirmUploadRequest {

    @NotBlank(message = "rawS3Key is required")
    @Size(max = 1024)
    private String rawS3Key;

    /** Optional; e.g. video/mp4. Defaults to application/octet-stream. */
    @Size(max = 128)
    private String contentType;
}
