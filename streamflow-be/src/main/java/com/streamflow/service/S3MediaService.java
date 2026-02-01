package com.streamflow.service;

import com.streamflow.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Core S3 operations for media: presigned upload URLs, key conventions,
 * existence checks, and cleanup. Used by API (presigned URLs) and optionally
 * by workers (upload processed files).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class S3MediaService {

    private static final String RAW_PREFIX = "raw/";
    private static final String PACKAGED_PREFIX = "packaged/";
    private static final String SPRITES_PREFIX = "sprites/";
    private static final String RAW_SOURCE_SUFFIX = "/source";
    private static final String PACKAGED_SUFFIX = "/";
    private static final String SPRITE_SUFFIX = "/thumb.jpg";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final AwsProperties aws;

    /**
     * Result of generating a presigned upload URL: the URL and the S3 key to store
     * in IngestionJob.
     */
    public record PresignedUploadResult(String uploadUrl, String s3Key) {
    }

    /**
     * Generate a presigned PUT URL for the frontend to upload raw video. The key is
     * deterministic
     * from videoAssetId so the backend can create the ingestion job with the same
     * key.
     */
    public PresignedUploadResult generatePresignedUploadUrl(UUID videoAssetId, String contentType) {
        String bucket = aws.getRawBucket();
        String key = buildRawVideoKey(videoAssetId);
        Duration expiry = Duration.ofMinutes(aws.getPresignedUploadExpiryMinutes());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String url = presigned.url().toString();
        log.debug("Presigned upload URL for videoAssetId={}, key={}, expiryMinutes={}", videoAssetId, key,
                aws.getPresignedUploadExpiryMinutes());
        return new PresignedUploadResult(url, key);
    }

    /**
     * S3 key for the raw upload object. Stored in IngestionJob.rawS3Key.
     */
    public String buildRawVideoKey(UUID videoAssetId) {
        return RAW_PREFIX + videoAssetId + RAW_SOURCE_SUFFIX;
    }

    /**
     * Path prefix for packaged output (HLS/DASH, manifest) for this asset. Worker
     * writes under this prefix.
     */
    public String buildPackagedVideoPath(UUID videoAssetId) {
        return PACKAGED_PREFIX + videoAssetId + PACKAGED_SUFFIX;
    }

    /**
     * S3 key for the sprite/thumbnail image for this asset.
     */
    public String buildSpritePath(UUID videoAssetId) {
        return SPRITES_PREFIX + videoAssetId + SPRITE_SUFFIX;
    }

    /**
     * Check if an object exists in the raw bucket. Useful to validate upload
     * completion.
     */
    public boolean objectExists(String bucket, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Check if the raw object for this video asset exists.
     */
    public boolean rawObjectExists(UUID videoAssetId) {
        return objectExists(aws.getRawBucket(), buildRawVideoKey(videoAssetId));
    }

    /**
     * Delete an object (e.g. cleanup after failed ingestion).
     */
    public void deleteObject(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        log.debug("Deleted s3://{}/{}", bucket, key);
    }

    /**
     * Delete the raw upload for this video asset. Use when ingestion fails and you
     * want to free space.
     */
    public void deleteRawUpload(UUID videoAssetId) {
        String key = buildRawVideoKey(videoAssetId);
        deleteObject(aws.getRawBucket(), key);
    }

    /**
     * Upload bytes to S3 (e.g. worker uploading a processed file). Use packaged
     * bucket from config
     * or pass bucket explicitly.
     */
    public void uploadBytes(String bucket, String key, byte[] data, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .contentLength((long) data.length)
                .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromBytes(data));
    }

    /**
     * Upload input stream to S3 (e.g. worker streaming a large file).
     */
    public void uploadStream(String bucket, String key, InputStream inputStream, long contentLength,
            String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .contentLength(contentLength)
                .build(),
                software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, contentLength));
    }

    /**
     * Raw bucket name (for callers that need it).
     */
    public String getRawBucket() {
        return aws.getRawBucket();
    }

    /**
     * Packaged bucket name (for worker or API that builds packaged URLs).
     */
    public String getPackagedBucket() {
        return aws.getPackagedBucket();
    }
}
