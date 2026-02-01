package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin signed URL audit item: metadata only (no active signed URL for safer demo logs).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignedUrlAuditItemResponse {

    private UUID id;
    private UUID videoAssetId;
    private UUID licenseId;
    private String urlType;
    private Instant expiresAt;
    private Instant createdAt;
}
