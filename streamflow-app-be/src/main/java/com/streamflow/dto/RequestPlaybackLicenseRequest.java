package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/playback/license.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestPlaybackLicenseRequest {

    @NotNull(message = "videoAssetId is required")
    private UUID videoAssetId;

    /** Optional device identifier for scoping. */
    private String deviceId;
}
