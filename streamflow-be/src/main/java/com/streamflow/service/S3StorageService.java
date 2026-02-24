package com.streamflow.service;

import com.streamflow.dto.UploadUrlResponse;
import com.streamflow.exception.S3UploadException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for uploading images and videos to Amazon S3.
 * Active only when S3 is enabled and {@link S3Client} is available.
 */
@Service
@Slf4j
@Data
@ConditionalOnBean(S3Client.class)
public class S3StorageService {

    /** Default prefix for image keys when none is provided. */
    public static final String DEFAULT_IMAGES_PREFIX = "images/";

    /** Default prefix for video keys when none is provided. */
    public static final String DEFAULT_VIDEOS_PREFIX = "videos/";

    /** Default prefix for sprite sheet images. */
    public static final String DEFAULT_SPRITES_PREFIX = "sprites/";

    /** S3 minimum part size (5 MB) except for the last part. */
    public static final long MIN_PART_SIZE_BYTES = 5L * 1024 * 1024;

    /** S3 maximum part size (5 GB). */
    public static final long MAX_PART_SIZE_BYTES = 5L * 1024 * 1024 * 1024;

    /** S3 maximum number of parts per multipart upload. */
    public static final int MAX_PARTS = 10_000;

    /** Content types accepted for video uploads (prefix or exact). */
    private static final String VIDEO_CONTENT_TYPE_PREFIX = "video/";
    private static final String OCTET_STREAM = "application/octet-stream";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${streamflow.aws.s3.bucket:}")
    private String bucket;

    @Value("${streamflow.aws.s3.images-prefix:" + DEFAULT_IMAGES_PREFIX + "}")
    private String imagesPrefix;

    @Value("${streamflow.aws.s3.videos-prefix:" + DEFAULT_VIDEOS_PREFIX + "}")
    private String videosPrefix;

    @Value("${streamflow.aws.s3.sprites-prefix:" + DEFAULT_SPRITES_PREFIX + "}")
    private String spritesPrefix;

    @Value("${streamflow.aws.s3.max-file-size-mb:512}")
    private long maxFileSizeMb;

    /**
     * Files larger than this (MB) must use presigned multipart upload; backend
     * upload is blocked. Keeps memory and timeouts safe for large files.
     */
    @Value("${streamflow.aws.s3.large-file-threshold-mb:100}")
    private long largeFileThresholdMb;

    @Value("${streamflow.ingestion.presigned-url-expiration-minutes:15}")
    private int presignedUrlExpirationMinutes;

    /** Presigned URL validity for multipart UploadPart requests (minutes). */
    @Value("${streamflow.ingestion.presigned-url-expiration-minutes:15}")
    private int presignedPartUrlExpirationMinutes;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Uploads a file from an input stream to S3. For files larger than
     * {@code streamflow.aws.s3.large-file-threshold-mb}, use presigned multipart
     * upload instead; this method will throw with a clear message.
     *
     * @param key           object key (e.g. "images/abc.jpg" or "videos/xyz.mp4")
     * @param inputStream   source stream (will not be closed by this method)
     * @param contentLength length in bytes; use -1 if unknown (may affect multipart
     *                      behaviour)
     * @param contentType   e.g. "image/jpeg", "video/mp4"
     * @param metadata      optional custom metadata; can be null
     * @return the S3 object key (same as input key, or generated)
     * @throws S3UploadException if bucket is not configured, file too large for
     *                           backend upload, or S3 returns an error
     */
    public String upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType,
            Map<String, String> metadata) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (contentLength >= 0) {
            rejectLargeFileForBackendUpload(contentLength, normalizedKey);
            validateSize(contentLength, normalizedKey);
        }

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .contentType(contentType != null ? contentType : OCTET_STREAM);

        if (metadata != null && !metadata.isEmpty()) {
            requestBuilder.metadata(metadata);
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
     * Key is generated using prefix (images/ or videos/) and original filename with
     * a UUID to avoid collisions. Files larger than the large-file threshold must
     * use presigned multipart upload; this method will throw with a clear message.
     *
     * @param file            the uploaded file
     * @param useVideosPrefix true to use videos prefix, false for images
     * @return the S3 object key
     * @throws S3UploadException if upload fails, file is empty/too large, or
     *                           file exceeds backend upload limit
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
                .orElse(OCTET_STREAM);

        if (useVideosPrefix) {
            validateVideoContentType(contentType, key);
        }

        try {
            long size = file.getSize();
            rejectLargeFileForBackendUpload(size, key);
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
     * Uploads a video (multipart). Uses videos prefix. Validates content type and
     * rejects files larger than the backend upload threshold (use presigned
     * multipart for large files).
     */
    public String uploadVideo(MultipartFile file) {
        return uploadMultipart(file, true);
    }

    /**
     * Server-side upload of raw video: client POSTs the file to the backend, and
     * the backend uploads to S3 using credentials. No presigned URL or CORS on S3
     * needed. Files larger than the large-file threshold must use presigned
     * multipart upload; this method will throw with a clear message.
     * Key is generated the same as for presigned flow:
     * videos/raw/{videoAssetId}/{uuid}.
     * Return value matches {@link UploadUrlResponse} so the client can call
     * confirm-upload with {@code rawS3Key} (uploadUrl is null).
     */
    public UploadUrlResponse uploadRawVideo(UUID videoAssetId, MultipartFile file) {
        ensureBucketConfigured();
        if (file == null || file.isEmpty()) {
            throw new S3UploadException("Upload file is null or empty", bucket, null);
        }
        String rawS3Key = generateRawVideoKey(videoAssetId);
        String contentType = Optional.ofNullable(file.getContentType())
                .filter(StringUtils::hasText)
                .orElse(OCTET_STREAM);
        validateVideoContentType(contentType, rawS3Key);
        try {
            long size = file.getSize();
            rejectLargeFileForBackendUpload(size, rawS3Key);
            validateSize(size, rawS3Key);
            try (InputStream is = file.getInputStream()) {
                upload(rawS3Key, is, size, contentType, null);
            }
        } catch (IOException e) {
            throw new S3UploadException(
                    "Failed to read multipart file for upload: " + e.getMessage(),
                    bucket, rawS3Key, e);
        }
        Instant expiration = Instant.now().plus(Duration.ofMinutes(presignedUrlExpirationMinutes));
        return UploadUrlResponse.builder()
                .uploadUrl(null)
                .rawS3Key(rawS3Key)
                .expiration(expiration)
                .build();
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
     * Generates a presigned PUT URL for client-side upload. Caller provides the raw
     * S3 key
     * (e.g. from generateRawVideoKey). Backend never handles raw video bytes.
     *
     * @param rawS3Key          object key (e.g. videos/raw/{videoAssetId}/{uuid})
     * @param expirationMinutes validity in minutes (max 7 days for standard S3)
     * @return uploadUrl, rawS3Key, expiration
     */
    public UploadUrlResponse generatePresignedPutUrl(String rawS3Key, int expirationMinutes) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(rawS3Key);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .putObjectRequest(putRequest)
                .build();
        var presigned = s3Presigner.presignPutObject(presignRequest);
        Instant expiration = Instant.now().plus(Duration.ofMinutes(expirationMinutes));
        log.info("Generated presigned PUT URL for bucket={}, key={}, expires={}", bucket, normalizedKey, expiration);
        return UploadUrlResponse.builder()
                .uploadUrl(presigned.url().toString())
                .rawS3Key(normalizedKey)
                .expiration(expiration)
                .build();
    }

    // ---------- Multipart upload (server-orchestrated, client-executed via
    // presigned URLs) ----------

    /**
     * Initiates a multipart upload. Returns an upload ID to use for
     * {@link #generatePresignedUploadPartUrls} and {@link #completeMultipartUpload}
     * or {@link #abortMultipartUpload}. Safe for 2GB+ files; backend does not
     * stream bytes.
     *
     * @param bucket      S3 bucket name (use this service's bucket or override)
     * @param key         object key (normalized; no leading slash)
     * @param contentType content type (e.g. video/mp4)
     * @return upload ID from S3
     * @throws S3UploadException if bucket/key invalid or S3 returns an error
     */
    public String initiateMultipartUpload(String bucket, String key, String contentType) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(bucket)) {
            throw new S3UploadException("Bucket must not be null or blank for multipart upload", bucket, normalizedKey);
        }
        try {
            CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .contentType(contentType != null ? contentType : OCTET_STREAM)
                    .build();
            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
            String uploadId = response.uploadId();
            log.info("Multipart upload initiated: bucket={}, key={}, uploadId={}, contentType={}",
                    bucket, normalizedKey, uploadId, contentType);
            return uploadId;
        } catch (S3Exception e) {
            throw mapS3Exception(e, bucket, normalizedKey, "initiateMultipartUpload");
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during initiateMultipartUpload for key " + normalizedKey + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Generates presigned UploadPart URLs for a multipart upload. Client uploads
     * each part with PUT to the URL and uses the response ETag in
     * {@link #completeMultipartUpload}. Supports resumable uploads (request URLs
     * for a subset of parts). Part sizes must comply with S3 limits (min 5 MB
     * except last, max 5 GB).
     *
     * @param uploadId  from {@link #initiateMultipartUpload}
     * @param key       same key used at initiation (normalized)
     * @param partSizes content length per part in order (part 1, part 2, ...);
     *                  validated against MIN_PART_SIZE_BYTES and
     *                  MAX_PART_SIZE_BYTES
     * @return list of presigned URLs in part order (index 0 = part 1, etc.)
     * @throws S3UploadException if validation fails or presigning fails
     */
    public List<String> generatePresignedUploadPartUrls(String uploadId, String key, List<Long> partSizes) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(uploadId)) {
            throw new S3UploadException("Upload ID must not be null or blank", bucket, normalizedKey);
        }
        validatePartSizes(partSizes, normalizedKey);

        Duration signatureDuration = Duration.ofMinutes(presignedPartUrlExpirationMinutes);
        List<String> urls = new ArrayList<>(partSizes.size());
        try {
            for (int i = 0; i < partSizes.size(); i++) {
                int partNumber = i + 1;
                long contentLength = partSizes.get(i);
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(normalizedKey)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength(contentLength)
                        .build();
                UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                        .signatureDuration(signatureDuration)
                        .uploadPartRequest(uploadPartRequest)
                        .build();
                String url = s3Presigner.presignUploadPart(presignRequest).url().toString();
                urls.add(url);
            }
            log.info("Generated presigned UploadPart URLs: uploadId={}, key={}, partCount={}",
                    uploadId, normalizedKey, partSizes.size());
            return urls;
        } catch (S3Exception e) {
            throw mapS3ExceptionWithContext(e, bucket, normalizedKey, "generatePresignedUploadPartUrls", uploadId,
                    null);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during generatePresignedUploadPartUrls for uploadId=" + uploadId + ", key="
                            + normalizedKey + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Uploads a single part from the server (e.g. for backend-orchestrated
     * multipart upload). Returns the ETag to use in
     * {@link #completeMultipartUpload}.
     *
     * @param bucket        S3 bucket name
     * @param key           same key used at initiation
     * @param uploadId      from {@link #initiateMultipartUpload}
     * @param partNumber    1-based part number
     * @param partContent   part body (will not be closed by this method)
     * @param contentLength part size in bytes
     * @return ETag returned by S3 for this part
     * @throws S3UploadException if upload fails
     */
    public String uploadPart(String bucket, String key, String uploadId, int partNumber,
            InputStream partContent, long contentLength) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(uploadId)) {
            throw new S3UploadException("Upload ID must not be null or blank", bucket, normalizedKey);
        }
        try {
            UploadPartRequest request = UploadPartRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .uploadId(uploadId)
                    .partNumber(partNumber)
                    .contentLength(contentLength)
                    .build();
            RequestBody body = RequestBody.fromInputStream(partContent, contentLength);
            UploadPartResponse response = s3Client.uploadPart(request, body);
            String etag = response.eTag();
            log.debug("Uploaded part: uploadId={}, partNumber={}, etag={}", uploadId, partNumber, etag);
            return etag;
        } catch (S3Exception e) {
            throw mapS3ExceptionWithContext(e, bucket, normalizedKey, "uploadPart", uploadId, partNumber);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during uploadPart for uploadId=" + uploadId + ", partNumber=" + partNumber + ": "
                            + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Completes a multipart upload. Call after the client has uploaded all parts
     * and collected ETags. If this fails, call {@link #abortMultipartUpload} to
     * clean up.
     *
     * @param uploadId       from {@link #initiateMultipartUpload}
     * @param key            same key used at initiation
     * @param completedParts list of CompletedPart (partNumber, eTag) in any order
     * @throws S3UploadException if completion fails
     */
    public void completeMultipartUpload(String uploadId, String key, List<CompletedPart> completedParts) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(uploadId)) {
            throw new S3UploadException("Upload ID must not be null or blank", bucket, normalizedKey);
        }
        if (completedParts == null || completedParts.isEmpty()) {
            throw new S3UploadException("Completed parts list must not be empty for completeMultipartUpload", bucket,
                    normalizedKey);
        }
        try {
            CompletedMultipartUpload completed = CompletedMultipartUpload.builder()
                    .parts(completedParts)
                    .build();
            CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .uploadId(uploadId)
                    .multipartUpload(completed)
                    .build();
            s3Client.completeMultipartUpload(request);
            log.info("Multipart upload completed: uploadId={}, key={}, partCount={}",
                    uploadId, normalizedKey, completedParts.size());
        } catch (S3Exception e) {
            throw mapS3ExceptionWithContext(e, bucket, normalizedKey, "completeMultipartUpload", uploadId, null);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during completeMultipartUpload for uploadId=" + uploadId + ", key=" + normalizedKey
                            + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Aborts a multipart upload. Call when the client will not complete the
     * upload (e.g. after failure or cancellation) to avoid leaving incomplete
     * uploads and incurring storage costs.
     *
     * @param uploadId from {@link #initiateMultipartUpload}
     * @param key      same key used at initiation
     * @throws S3UploadException if abort fails
     */
    public void abortMultipartUpload(String uploadId, String key) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        if (!StringUtils.hasText(uploadId)) {
            throw new S3UploadException("Upload ID must not be null or blank", bucket, normalizedKey);
        }
        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(normalizedKey)
                    .uploadId(uploadId)
                    .build();
            s3Client.abortMultipartUpload(request);
            log.info("Multipart upload aborted: uploadId={}, key={}", uploadId, normalizedKey);
        } catch (S3Exception e) {
            throw mapS3ExceptionWithContext(e, bucket, normalizedKey, "abortMultipartUpload", uploadId, null);
        } catch (software.amazon.awssdk.core.exception.SdkClientException e) {
            throw new S3UploadException(
                    "S3 client error during abortMultipartUpload for uploadId=" + uploadId + ", key=" + normalizedKey
                            + ": " + e.getMessage(),
                    bucket, normalizedKey, e);
        }
    }

    /**
     * Generates a unique raw video S3 key for a video asset. Use this when creating
     * upload URL so the client uploads to a known key to be passed to
     * confirm-upload.
     */
    public String generateRawVideoKey(UUID videoAssetId) {
        String prefix = StringUtils.hasText(videosPrefix) ? videosPrefix : DEFAULT_VIDEOS_PREFIX;
        return prefix + "raw/" + videoAssetId + "/" + UUID.randomUUID();
    }

    /**
     * Opens an S3 object for streaming (e.g. HLS manifest or segment).
     * Caller must close the stream when done.
     *
     * @param key S3 object key
     * @return response input stream; use stream.response() for Content-Type, Content-Length
     */
    public ResponseInputStream<GetObjectResponse> getObjectStream(String key) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        return s3Client.getObject(request);
    }

    /**
     * Downloads an S3 object to a temporary file. Caller is responsible for deleting
     * the file when done (e.g. in a try-finally or try-with-resources).
     *
     * @param key S3 object key (e.g. raw video key)
     * @return path to the temporary file
     * @throws IOException if download or write fails
     */
    public Path downloadToTempFile(String key) throws IOException {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        Path tempFile = Files.createTempFile("streamflow-video-", ".mp4");
        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(request)) {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Downloaded S3 object to temp file: bucket={}, key={}", bucket, normalizedKey);
        return tempFile;
    }

    /**
     * Returns the public or pre-signed URL for an object if you use a custom domain
     * or CDN.
     * This implementation returns the default S3 object URL (bucket + key).
     * Override or use
     * a separate URL builder if you use CloudFront or custom domain.
     */
    public String getObjectUrl(String key) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, normalizedKey);
    }

    /**
     * Generates a short-lived presigned GET URL for secure playback (manifest or
     * segment).
     *
     * @param key               S3 object key (e.g. manifest path or segment path)
     * @param expirationMinutes validity in minutes (keep short for playback)
     * @return presigned URL and expiration instant
     */
    public PresignedGetUrlResult generatePresignedGetUrl(String key, int expirationMinutes) {
        ensureBucketConfigured();
        String normalizedKey = normalizeKey(key);
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(normalizedKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getRequest)
                .build();
        var presigned = s3Presigner.presignGetObject(presignRequest);
        Instant expiration = Instant.now().plus(Duration.ofMinutes(expirationMinutes));
        log.debug("Generated presigned GET URL for bucket={}, key={}, expires={}", bucket, normalizedKey, expiration);
        return new PresignedGetUrlResult(presigned.url().toString(), expiration);
    }

    /** Result of presigned GET URL generation for playback. */
    public static final class PresignedGetUrlResult {
        private final String url;
        private final Instant expiresAt;

        public PresignedGetUrlResult(String url, Instant expiresAt) {
            this.url = url;
            this.expiresAt = expiresAt;
        }

        public String getUrl() {
            return url;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    private void ensureBucketConfigured() {
        if (!StringUtils.hasText(bucket)) {
            throw new S3UploadException(
                    "S3 bucket is not configured. Set streamflow.aws.s3.bucket (or AWS_S3_BUCKET).",
                    null, null);
        }
    }

    /**
     * Rejects uploads larger than the backend threshold. Such files must use
     * presigned multipart upload; backend must not stream them.
     */
    private void rejectLargeFileForBackendUpload(long bytes, String key) {
        long thresholdBytes = largeFileThresholdMb * 1024 * 1024;
        if (bytes > thresholdBytes) {
            throw new S3UploadException(
                    String.format(
                            "File size %d bytes (%.1f MB) exceeds backend upload limit %d MB for key %s. Use presigned multipart upload for large files (initiateMultipartUpload, generatePresignedUploadPartUrls, completeMultipartUpload).",
                            bytes, bytes / (1024.0 * 1024.0), largeFileThresholdMb, key),
                    bucket, key);
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

    /**
     * Validates content type for video uploads (video/* or
     * application/octet-stream).
     */
    private void validateVideoContentType(String contentType, String key) {
        if (!StringUtils.hasText(contentType)) {
            return;
        }
        String ct = contentType.trim().toLowerCase();
        if (ct.startsWith(VIDEO_CONTENT_TYPE_PREFIX) || OCTET_STREAM.equals(ct)) {
            return;
        }
        throw new S3UploadException(
                "Invalid content type for video upload: " + contentType + ". Expected video/* or " + OCTET_STREAM
                        + " for key " + key,
                bucket, key);
    }

    /**
     * Validates multipart part sizes (S3: min 5 MB except last, max 5 GB; max 10000
     * parts).
     */
    private void validatePartSizes(List<Long> partSizes, String key) {
        if (partSizes == null || partSizes.isEmpty()) {
            throw new S3UploadException("Part sizes list must not be null or empty", bucket, key);
        }
        if (partSizes.size() > MAX_PARTS) {
            throw new S3UploadException(
                    "Part count " + partSizes.size() + " exceeds S3 maximum " + MAX_PARTS + " for key " + key,
                    bucket, key);
        }
        for (int i = 0; i < partSizes.size(); i++) {
            long size = partSizes.get(i);
            int partNumber = i + 1;
            if (size <= 0) {
                throw new S3UploadException(
                        "Part size must be positive for partNumber=" + partNumber + ", key=" + key,
                        bucket, key);
            }
            if (size > MAX_PART_SIZE_BYTES) {
                throw new S3UploadException(
                        String.format("Part size %d bytes exceeds S3 maximum %d MB for partNumber=%d, key=%s",
                                size, MAX_PART_SIZE_BYTES / (1024 * 1024), partNumber, key),
                        bucket, key);
            }
            boolean isLastPart = (i == partSizes.size() - 1);
            if (!isLastPart && size < MIN_PART_SIZE_BYTES) {
                throw new S3UploadException(
                        String.format("Part size %d bytes below S3 minimum %d MB for non-last partNumber=%d, key=%s",
                                size, MIN_PART_SIZE_BYTES / (1024 * 1024), partNumber, key),
                        bucket, key);
            }
        }
    }

    private String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) {
            throw new S3UploadException("S3 object key must not be null or blank", bucket, key);
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            throw new S3UploadException("S3 object key must not be blank", bucket, key);
        }
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }

    private static S3UploadException mapS3Exception(S3Exception e, String bucket, String key, String operation) {
        String message = String.format("S3 %s failed: %s (code: %s)", operation, e.awsErrorDetails().errorMessage(),
                e.awsErrorDetails().errorCode());
        return new S3UploadException(message, bucket, key, e);
    }

    private static S3UploadException mapS3ExceptionWithContext(S3Exception e, String bucket, String key,
            String operation, String uploadId, Integer partNumber) {
        String ctx = (uploadId != null ? ", uploadId=" + uploadId : "")
                + (partNumber != null ? ", partNumber=" + partNumber : "");
        String message = String.format("S3 %s failed: %s (code: %s)%s", operation, e.awsErrorDetails().errorMessage(),
                e.awsErrorDetails().errorCode(), ctx);
        return new S3UploadException(message, bucket, key, e);
    }
}
