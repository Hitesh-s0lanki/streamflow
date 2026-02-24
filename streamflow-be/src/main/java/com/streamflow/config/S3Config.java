package com.streamflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 configuration for clients and presigner, with shared settings.
 */
@Configuration
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Value("${streamflow.aws.s3.region:us-east-1}")
    private String region;

    @Value("${streamflow.aws.s3.endpoint-override:}")
    private String endpointOverride;

    @Value("${streamflow.aws.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${streamflow.aws.s3.secret-access-key:}")
    private String secretAccessKey;

    @Value("${streamflow.aws.s3.path-style-access:false}")
    private boolean pathStyleAccess;

    /**
     * Shared S3 service configuration (path-style). Applied to sync, async, and
     * presigner.
     */
    private S3Configuration s3ServiceConfiguration() {
        return pathStyleAccess
                ? S3Configuration.builder().pathStyleAccessEnabled(true).build()
                : S3Configuration.builder().build();
    }

    /** Shared credentials. Used by all S3 clients and presigner. */
    private AwsCredentialsProvider resolveCredentials() {
        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        return DefaultCredentialsProvider.create();
    }

    /** Shared region. */
    private Region resolveRegion() {
        return Region.of(region);
    }

    /** Shared endpoint override URI, or null if not set. */
    private URI resolveEndpointOverride() {
        if (endpointOverride == null || endpointOverride.isBlank()) {
            return null;
        }
        try {
            return URI.create(endpointOverride);
        } catch (Exception e) {
            log.warn("Invalid S3 endpoint override '{}': {}", endpointOverride, e.getMessage());
            return null;
        }
    }

    @Bean
    @ConditionalOnProperty(name = "streamflow.aws.s3.enabled", havingValue = "true")
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(resolveRegion())
                .serviceConfiguration(s3ServiceConfiguration());

        AwsCredentialsProvider credentials = resolveCredentials();
        if (credentials != null) {
            builder.credentialsProvider(credentials);
        }
        URI endpoint = resolveEndpointOverride();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }

        S3Client client = builder.build();
        log.info("S3 sync client configured: region={}, endpointOverride={}", region,
                endpoint != null ? endpointOverride : "default");
        return client;
    }

    @Bean
    @ConditionalOnProperty(name = "streamflow.aws.s3.enabled", havingValue = "true")
    public S3AsyncClient s3AsyncClient() {
        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .region(resolveRegion())
                .serviceConfiguration(s3ServiceConfiguration());

        AwsCredentialsProvider credentials = resolveCredentials();
        if (credentials != null) {
            builder.credentialsProvider(credentials);
        }
        URI endpoint = resolveEndpointOverride();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }

        S3AsyncClient asyncClient = builder.build();
        log.info("S3 async client configured: region={}, endpointOverride={}", region,
                endpoint != null ? endpointOverride : "default");
        return asyncClient;
    }

    @Bean
    @ConditionalOnProperty(name = "streamflow.aws.s3.enabled", havingValue = "true")
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(resolveRegion())
                .serviceConfiguration(s3ServiceConfiguration());

        AwsCredentialsProvider credentials = resolveCredentials();
        if (credentials != null) {
            builder.credentialsProvider(credentials);
        }
        URI endpoint = resolveEndpointOverride();
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }

        S3Presigner presigner = builder.build();
        log.info("S3 presigner configured: region={}, endpointOverride={}", region,
                endpoint != null ? endpointOverride : "default");
        log.info("S3 clients configured: sync=true, async=true, presigner=true");
        return presigner;
    }
}
