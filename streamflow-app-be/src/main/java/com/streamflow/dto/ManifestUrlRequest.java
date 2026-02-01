package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/playback/{videoAssetId}/manifest-url.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManifestUrlRequest {

    @NotNull(message = "licenseId is required")
    private UUID licenseId;
}
