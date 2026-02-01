package com.streamflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * Configures AWS SDK v2 clients. S3 and S3Presigner are created only when
 * app.aws.raw-bucket is set, so the app can run without AWS in tests.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {

    @Bean
    @ConditionalOnProperty(name = "app.aws.raw-bucket")
    S3Client s3Client(AwsProperties aws) {
        var builder = S3Client.builder().region(Region.of(aws.getRegion()));
        if (aws.getEndpointOverride() != null && !aws.getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(aws.getEndpointOverride()));
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.aws.raw-bucket")
    S3Presigner s3Presigner(AwsProperties aws) {
        var builder = S3Presigner.builder().region(Region.of(aws.getRegion()));
        if (aws.getEndpointOverride() != null && !aws.getEndpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(aws.getEndpointOverride()));
        }
        return builder.build();
    }
}
