package com.streamflow.worker.service;

import com.streamflow.config.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Downloads raw video from S3 to local temp disk. Stream-safe (no full memory
 * load).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class S3DownloadService {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    /**
     * Download the object at rawS3Key from the raw bucket to a local file.
     *
     * @param rawS3Key  S3 key (e.g. raw/{videoAssetId}/source)
     * @param localPath local path to write to (parent dirs created if needed)
     * @return the path to the downloaded file
     */
    public Path downloadToFile(String rawS3Key, Path localPath) {
        String bucket = awsProperties.getRawBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.aws.raw-bucket is not set");
        }
        try {
            Path parent = localPath.getParent();
            if (parent != null && !java.nio.file.Files.exists(parent)) {
                java.nio.file.Files.createDirectories(parent);
            }
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(rawS3Key)
                    .build();
            s3Client.getObject(req, ResponseTransformer.toFile(localPath));
            log.info("Downloaded s3://{}/{} to {}", bucket, rawS3Key, localPath);
            return localPath;
        } catch (NoSuchKeyException e) {
            throw new IllegalArgumentException("Raw object not found: s3://" + bucket + "/" + rawS3Key, e);
        } catch (S3Exception e) {
            throw new RuntimeException("S3 download failed: " + e.awsErrorDetails(), e);
        }
    }

    /**
     * Download to a path under tempDir: {tempDir}/{videoAssetId}/source (filename
     * from key or "source").
     */
    public Path downloadToTemp(String rawS3Key, java.util.UUID videoAssetId, String tempDir) {
        String fileName = Paths.get(rawS3Key).getFileName().toString();
        if (fileName == null || fileName.isBlank()) {
            fileName = "source";
        }
        Path localPath = Paths.get(tempDir, videoAssetId.toString(), fileName);
        return downloadToFile(rawS3Key, localPath);
    }
}
