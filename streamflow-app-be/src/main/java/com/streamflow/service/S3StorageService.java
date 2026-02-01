package com.streamflow.service;

import com.streamflow.exception.S3UploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for uploading images and videos to Amazon S3.
 * Active only when S3 is enabled and {@link S3Client} is available.
 */
@Service
@ConditionalOnBean(S3Client.class)
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    /** Default prefix for image keys when none is provided. */
    public static final String DEFAULT_IMAGES_PREFIX = "images/";

    /** Default prefix for video keys when none is provided. */
    public static final String DEFAULT_VIDEOS_PREFIX = "videos/";

    private final S3Client s3Client;

    @Value("${streamflow.aws.s3.bucket:}")
    private String bucket;

    @Value("${streamflow.aws.s3.images-prefix:" + DEFAULT_IMAGES_PREFIX + "}")
    private String imagesPrefix;

    @Value("${streamflow.aws.s3.videos-prefix:" + DEFAULT_VIDEOS_PREFIX + "}")
    private String videosPrefix;

    @Value("${streamflow.aws.s3.max-file-size-mb:512}")
    private long maxFileSizeMb;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a file from an input stream to S3.
     *
     * @param key         object key (e.g. "images/abc.jpg" or "videos/xyz.mp4")
     * @param inputStream source stream (will not be closed by this method)
     * @param contentLength length in bytes; use -1 if unknown (may affect multipart behaviour)
     * @param contentType  e.g. "image/jpeg", "video/mp4"
     * @param metadata    optional custom metadata; can be null
     * @return the S3 object key (same as input key, or generated)
     * @throws S3UploadException if bucket is not configured, or S3 returns an error
     */
    public String upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType,
            Map<String, String> metadata) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .contentType(contentType != null ? contentType : "application/octet-stream");

        if (metadata != null && !metadata.isEmpty()) {
            requestBuilder.metadata(metadata);
        }

        if (contentLength >= 0) {
            validateSize(contentLength, normalizedKey);
        }

        try {
            RequestBody body = contentLength >= 0
                    ? RequestBody.fromInputStream(inputStream, contentLength)
                    : RequestBody.fromInputStream(inputStream, -1);
            s3Client.putObject(requestBuilder.build(), body);
            log.info("Uploaded to S3: bucket={}, key={}, contentType={}", bucket, normalizedKey, contentType);
            return normalizedKey;
        } catch (S3Exception e) {
            throw mapS3Exception(e, bucket, normalizedKey, "upload");
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during upload for key " + normalizedKey + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Uploads a multipart file (e.g. from a controller) as an image or video.
     * Key is generated using prefix (images/ or videos/) and original filename with a UUID to avoid collisions.
     *
     * @param file        the uploaded file
     * @param useVideosPrefix true to use videos prefix, false for images
     * @return the S3 object key
     * @throws S3UploadException if upload fails or file is empty/too large
     */
    public String uploadMultipart(MultipartFile file, boolean useVideosPrefix) {
        ensureBucketConfigured();
        if (file == null || file.isEmpty()) {
            throw new S3UploadException("Upload file is null or empty", bucket, null);
        }

        String prefix = useVideosPrefix ? videosPrefix : imagesPrefix;
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "file";
        String safeName = StringUtils.cleanPath(originalName).replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = prefix + UUID.randomUUID() + "-" + safeName;

        String contentType = Optional.ofNullable(file.getContentType())
                .filter(StringUtils::hasText)
                .orElse("application/octet-stream");

        try {
            long size = file.getSize();
            validateSize(size, key);
            try (InputStream is = file.getInputStream()) {
                return upload(key, is, size, contentType, null);
            }
        } catch (IOException e) {
            throw new S3UploadException(
                    "Failed to read multipart file for upload: " + e.getMessage(),
                    bucket, key, e);
        }
    }

    /**
     * Uploads an image (multipart). Uses images prefix.
     */
    public String uploadImage(MultipartFile file) {
        return uploadMultipart(file, false);
    }

    /**
     * Uploads a video (multipart). Uses videos prefix.
     */
    public String uploadVideo(MultipartFile file) {
        return uploadMultipart(file, true);
    }

    /**
     * Deletes an object from S3. Does not throw if the object does not exist.
     *
     * @param key object key
     * @throws S3UploadException (wrapping S3 errors) on failure
     */
    public void delete(String key) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .build());
            log.info("Deleted from S3: bucket={}, key={}", bucket, normalizedKey);
        } catch (S3Exception e) {
            throw mapS3Exception(e, bucket, normalizedKey, "delete");
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during delete for key " + normalizedKey + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Returns the public or pre-signed URL for an object if you use a custom domain or CDN.
     * This implementation returns the default S3 object URL (bucket + key). Override or use
     * a separate URL builder if you use CloudFront or custom domain.
     */
    public String getObjectUrl(String key) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, normalizedKey);
    }

    private void ensureBucketConfigured() {
        if (!StringUtils.hasText(bucket)) {
            throw new S3UploadException(
                    "S3 bucket is not configured. Set streamflow.aws.s3.bucket (or AWS_S3_BUCKET).",
                    null, null);
        }
    }

    private void validateSize(long bytes, String key) {
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (bytes > maxBytes) {
            throw new S3UploadException(
                    String.format("File size %d bytes exceeds maximum allowed %d MB for key %s",
                            bytes, maxFileSizeMb, key),
                    bucket, key);
        }
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new S3UploadException("S3 object key must not be null or blank", bucket, key);
        }
        return key.startsWith("/") ? key.substring(1) : key;
    }

    private static S3UploadException mapS3Exception(S3Exception e, String bucket, String key, String operation) {
        String message = String.format("S3 %s failed: %s (code: %s)", operation, e.awsErrorDetails().errorMessage(),
                e.awsErrorDetails().errorCode());
        return new S3UploadException(message, bucket, key, e);
    }
}
