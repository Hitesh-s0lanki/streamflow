package com.streamflow.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignedUploadResponse {

    /** URL for PUT upload (frontend uploads file here). */
    private String uploadUrl;

    /** S3 key (stored in IngestionJob.rawS3Key after upload). */
    private String s3Key;

    /** Expiry in minutes; upload must complete before this. */
    private int expiresInMinutes;
}
