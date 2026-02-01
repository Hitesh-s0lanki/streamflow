package com.streamflow.worker.service;

import com.streamflow.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Uploads packaged assets (HLS/DASH manifests and segments) and sprites to S3.
 * Prefix: s3://packaged-bucket/{videoAssetId}/ and
 * s3://packaged-bucket/sprites/{videoAssetId}/
 * or a dedicated sprites bucket if configured.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class S3UploadService {

    private static final String PACKAGED_PREFIX = "packaged/";
    private static final String SPRITES_PREFIX = "sprites/";

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    public String getPackagedBucket() {
        String b = awsProperties.getPackagedBucket();
        if (b == null || b.isBlank()) {
            return awsProperties.getRawBucket();
        }
        return b;
    }

    /**
     * Upload a single file to the packaged bucket under
     * packaged/{videoAssetId}/{relativePath}.
     */
    public String uploadPackagedFile(UUID videoAssetId, Path localFile, String s3RelativeKey) {
        String bucket = getPackagedBucket();
        String key = PACKAGED_PREFIX + videoAssetId + "/" + s3RelativeKey.replace("\\", "/");
        uploadFile(bucket, key, localFile, contentTypeForKey(s3RelativeKey));
        return "s3://" + bucket + "/" + key;
    }

    /**
     * Upload all files from a local directory to packaged/{videoAssetId}/.
     * Preserves relative paths under dir.
     */
    public void uploadPackagedDirectory(UUID videoAssetId, Path dir) throws IOException {
        String bucket = getPackagedBucket();
        String prefix = PACKAGED_PREFIX + videoAssetId + "/";
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                Path relative = dir.relativize(file);
                String key = prefix + relative.toString().replace("\\", "/");
                uploadFile(bucket, key, file, contentTypeForKey(relative.toString()));
            });
        }
        log.info("Uploaded packaged dir {} to s3://{}/{}", dir, bucket, prefix);
    }

    /**
     * Upload a sprite image to sprites/{videoAssetId}/sprite_001.jpg (or
     * filename).
     */
    public String uploadSprite(UUID videoAssetId, String filename, Path localFile) {
        String bucket = getPackagedBucket();
        String key = SPRITES_PREFIX + videoAssetId + "/" + filename;
        uploadFile(bucket, key, localFile, "image/jpeg");
        return "s3://" + bucket + "/" + key;
    }

    private void uploadFile(String bucket, String key, Path localFile, String contentType) {
        try {
            long size = Files.size(localFile);
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();
            s3Client.putObject(req, RequestBody.fromFile(localFile));
            log.debug("Uploaded {} to s3://{}/{}", localFile.getFileName(), bucket, key);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + localFile, e);
        }
    }

    private static String contentTypeForKey(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".mpd"))
            return "application/dash+xml";
        if (lower.endsWith(".m3u8"))
            return "application/vnd.apple.mpegurl";
        if (lower.endsWith(".m4s") || lower.endsWith(".mp4"))
            return "video/mp4";
        if (lower.endsWith(".ts"))
            return "video/mp2t";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
            return "image/jpeg";
        return "application/octet-stream";
    }
}
