package com.streamflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/video-assets/{videoAssetId}/manifest.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterManifestRequest {

    @NotBlank(message = "manifestUrl is required")
    private String manifestUrl;
}
