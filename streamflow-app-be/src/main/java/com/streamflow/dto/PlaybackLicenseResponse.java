package com.streamflow.dto;

import com.streamflow.entity.enums.LicenseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for license request and validation (POST/GET playback license).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackLicenseResponse {

    private UUID licenseId;
    private UUID videoAssetId;
    private String userId;
    private String deviceId;
    private Instant expiresAt;
    private LicenseStatus licenseStatus;
}
