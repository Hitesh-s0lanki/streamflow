package com.streamflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Centralized AWS configuration. No hardcoded region or bucket names;
 * all values are env-configurable for dev/stage/prod.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.aws")
public class AwsProperties {

    /**
     * AWS region (e.g. us-east-1). Used for S3 and CloudFront.
     */
    private String region = "us-east-1";

    /**
     * Bucket for raw uploads (client uploads via presigned URL).
     */
    private String rawBucket;

    /**
     * Bucket for processed/packaged output (HLS/DASH, sprites). Worker writes here.
     */
    private String packagedBucket;

    /**
     * Presigned upload URL validity in minutes. Frontend must complete upload
     * within this window.
     */
    private int presignedUploadExpiryMinutes = 15;

    /**
     * Optional custom endpoint (e.g. for LocalStack). Leave empty for real AWS.
     */
    private String endpointOverride;
}
