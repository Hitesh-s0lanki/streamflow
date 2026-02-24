package com.streamflow.service;

import com.streamflow.entity.Content;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.entity.enums.UploadStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.exception.VideoUploadException;
import com.streamflow.exception.VideoUploadException.UploadPhase;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.VideoAssetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Service for handling video uploads for movie content.
 * No raw video is stored in S3; the file is used only for HLS transcoding and
 * sprite generation, then discarded.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadService {

    private static final String VIDEO_CONTENT_TYPE_PREFIX = "video/";
    private static final String OCTET_STREAM = "application/octet-stream";

    private final ContentRepository contentRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final S3StorageService s3StorageService;
    private final HlsTranscodingService hlsTranscodingService;
    private final SpriteSheetService spriteSheetService;

    /**
     * Maximum allowed file size in MB.
     */
    @Value("${streamflow.aws.s3.max-file-size-mb:5120}")
    private long maxFileSizeMb;

    /**
     * Uploads a video file for a movie content.
     * Automatically chooses between direct upload and multipart upload based on
     * file size.
     *
     * NOT @Transactional: S3 uploads are long-running I/O operations and must not
     * hold a database connection/transaction open. Each repository.save() call
     * uses its own implicit transaction (Spring Data default).
     *
     * @param contentId the ID of the content (must be a MOVIE type)
     * @param videoFile the video file to upload
     * @return the created/updated VideoAsset with upload status
     * @throws ResourceNotFoundException if content not found
     * @throws BadRequestException       if content is not a MOVIE type or video
     *                                   already exists
     * @throws VideoUploadException      if upload fails at any stage
     */
    /**
     * Validates and runs HLS + sprite generation from the video file (no raw S3 upload).
     * Used for sync retry path.
     */
    public VideoAsset uploadVideoMovie(UUID contentId, MultipartFile videoFile) {
        VideoAsset videoAsset = validateAndPrepareUpload(contentId, videoFile);
        Path tempFile = null;

        try {
            tempFile = copyToTempFile(videoFile);

            videoAsset.markUploadStarted();
            videoAsset.setTotalParts(1);
            videoAsset.setUploadedParts(0);
            videoAsset = videoAssetRepository.save(videoAsset);

            runHlsAndSpritesFromTemp(videoAsset.getId(), tempFile);

            return videoAssetRepository.findById(videoAsset.getId()).orElseThrow();
        } catch (VideoUploadException e) {
            throw e;
        } catch (IOException e) {
            handleUploadFailure(videoAsset, "Failed to prepare video file: " + e.getMessage());
            throw new VideoUploadException(
                    "Failed to prepare video file for upload",
                    contentId, videoAsset.getId(), UploadPhase.INITIALIZATION, e);
        } catch (Exception e) {
            handleUploadFailure(videoAsset, "Unexpected error: " + e.getMessage());
            throw new VideoUploadException(
                    "Video processing failed: " + e.getMessage(),
                    contentId, videoAsset.getId(), UploadPhase.PART_UPLOAD, e);
        } finally {
            deleteTempFileSilently(tempFile);
        }
    }

    /**
     * Validates the upload request and prepares the VideoAsset entity.
     * Can be called independently to perform validation before async upload.
     *
     * @throws ResourceNotFoundException if content not found
     * @throws BadRequestException if validation fails (empty file, wrong type, size, etc.)
     */
    public VideoAsset validateAndPrepareUpload(UUID contentId, MultipartFile videoFile) {
        // Validate file is not empty
        if (videoFile == null || videoFile.isEmpty()) {
            throw new BadRequestException("Video file cannot be null or empty");
        }

        // Validate content type
        String contentType = videoFile.getContentType();
        if (!isValidVideoContentType(contentType)) {
            throw new BadRequestException(
                    "Invalid content type: " + contentType + ". Expected video/* or " + OCTET_STREAM);
        }

        // Validate file size
        long fileSize = videoFile.getSize();
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (fileSize > maxBytes) {
            throw new BadRequestException(
                    String.format("File size %.2f MB exceeds maximum allowed %.0f MB",
                            fileSize / (1024.0 * 1024.0), (double) maxFileSizeMb));
        }

        // Find and validate content
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        // Ensure content is a MOVIE
        if (content.getContentType() != ContentType.MOVIE) {
            throw new BadRequestException(
                    "Content type must be MOVIE for video upload. Found: " + content.getContentType());
        }

        // Check if video asset already exists
        VideoAsset videoAsset = videoAssetRepository.findByContentId(contentId).orElse(null);

        if (videoAsset != null) {
            // If a previous upload exists and is in progress, abort it first
            if (videoAsset.getUploadStatus() == UploadStatus.UPLOADING ||
                    videoAsset.getUploadStatus() == UploadStatus.MULTIPART_INITIATED) {
                abortExistingUpload(videoAsset);
            } else if (videoAsset.getUploadStatus() == UploadStatus.COMPLETED) {
                throw new BadRequestException(
                        "Video already uploaded for content: " + contentId +
                                ". Delete existing video first to upload a new one.");
            }
            // Reset the video asset for new upload
            resetVideoAssetForNewUpload(videoAsset, videoFile);
        } else {
            // Create new video asset
            videoAsset = createVideoAsset(content, videoFile);
        }

        return videoAssetRepository.save(videoAsset);
    }

    /**
     * Creates a new VideoAsset entity.
     */
    private VideoAsset createVideoAsset(Content content, MultipartFile videoFile) {
        VideoAsset videoAsset = new VideoAsset();
        videoAsset.setContent(content);
        videoAsset.setOriginalFilename(sanitizeFilename(videoFile.getOriginalFilename()));
        videoAsset.setContentType(videoFile.getContentType());
        videoAsset.setFileSizeBytes(videoFile.getSize());
        videoAsset.setUploadStatus(UploadStatus.PENDING);
        videoAsset.setDrmEnabled(false);
        return videoAsset;
    }

    /**
     * Resets an existing VideoAsset for a new upload attempt.
     */
    private void resetVideoAssetForNewUpload(VideoAsset videoAsset, MultipartFile videoFile) {
        videoAsset.setOriginalFilename(sanitizeFilename(videoFile.getOriginalFilename()));
        videoAsset.setContentType(videoFile.getContentType());
        videoAsset.setFileSizeBytes(videoFile.getSize());
        videoAsset.setUploadStatus(UploadStatus.PENDING);
        videoAsset.setRawS3Key(null);
        videoAsset.setMultipartUploadId(null);
        videoAsset.setUploadErrorMessage(null);
        videoAsset.setUploadStartedAt(null);
        videoAsset.setUploadCompletedAt(null);
        videoAsset.setTotalParts(null);
        videoAsset.setUploadedParts(null);
    }

    /**
     * Handles upload failure by updating the VideoAsset status.
     */
    private void handleUploadFailure(VideoAsset videoAsset, String errorMessage) {
        try {
            videoAsset.markUploadFailed(errorMessage);
            videoAssetRepository.save(videoAsset);
            log.error("Upload failed for videoAsset: {}, error: {}", videoAsset.getId(), errorMessage);
        } catch (Exception e) {
            log.error("Failed to update video asset status after upload failure: {}", e.getMessage());
        }
    }

    /**
     * Aborts an existing in-progress upload.
     */
    private void abortExistingUpload(VideoAsset videoAsset) {
        String uploadId = videoAsset.getMultipartUploadId();
        String rawS3Key = videoAsset.getRawS3Key();

        if (uploadId != null && rawS3Key != null) {
            try {
                s3StorageService.abortMultipartUpload(uploadId, rawS3Key);
            } catch (Exception e) {
                log.error("Failed to abort existing multipart upload. uploadId: {}, error: {}",
                        uploadId, e.getMessage());
            }
        }

        videoAsset.markCancelled();
        videoAssetRepository.save(videoAsset);
    }

    /**
     * Validates if the content type is a valid video type.
     */
    private boolean isValidVideoContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return true; // Allow null/empty, will default to octet-stream
        }
        String ct = contentType.trim().toLowerCase();
        return ct.startsWith(VIDEO_CONTENT_TYPE_PREFIX) || OCTET_STREAM.equals(ct);
    }

    /**
     * Sanitizes the filename to remove potentially dangerous characters.
     */
    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "video";
        }
        return org.springframework.util.StringUtils.cleanPath(filename)
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Runs HLS + sprite generation from the temp file in the background (no raw S3 upload).
     * Called after validation via {@link #validateAndPrepareUpload}. Temp file is cleaned up in finally.
     */
    @Async
    public void uploadVideoMovieAsync(UUID videoAssetId, Path tempVideoFile) {
        try {
            VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId)
                    .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));

            log.info("Starting async video processing (HLS + sprites) for videoAssetId={}, fileSize={} bytes",
                    videoAssetId, videoAsset.getFileSizeBytes());

            videoAsset.markUploadStarted();
            videoAsset.setTotalParts(1);
            videoAsset.setUploadedParts(0);
            videoAssetRepository.save(videoAsset);

            runHlsAndSpritesFromTemp(videoAssetId, tempVideoFile);
        } catch (Exception e) {
            log.error("Async video processing failed for videoAssetId={}: {}", videoAssetId, e.getMessage(), e);
            try {
                VideoAsset va = videoAssetRepository.findById(videoAssetId).orElse(null);
                if (va != null && va.getUploadStatus() != UploadStatus.FAILED) {
                    handleUploadFailure(va, e.getMessage());
                }
            } catch (Exception inner) {
                log.error("Failed to update upload status after async failure for videoAssetId={}: {}",
                        videoAssetId, inner.getMessage());
            }
        } finally {
            deleteTempFileSilently(tempVideoFile);
        }
    }

    /**
     * Runs HLS transcoding and sprite generation from the temp file. No raw video is stored in S3.
     * On success: marks processing and upload completed (rawS3Key remains null). On failure: marks both failed.
     */
    private void runHlsAndSpritesFromTemp(UUID videoAssetId, Path tempVideoFile) {
        try {
            VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId)
                    .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));

            videoAsset.markProcessingQueued();
            videoAssetRepository.save(videoAsset);
            videoAsset.markProcessingStarted();
            videoAssetRepository.save(videoAsset);

            int durationSeconds = spriteSheetService.getVideoDurationSeconds(tempVideoFile);
            log.info("HLS + sprites started for videoAssetId={}, duration={}s", videoAssetId, durationSeconds);

            hlsTranscodingService.transcode(videoAsset, tempVideoFile, durationSeconds);

            videoAsset = videoAssetRepository.findById(videoAssetId).orElseThrow();
            log.info("Sprite generation for videoAssetId={}", videoAssetId);
            spriteSheetService.processVideoAndGenerateSprites(videoAsset, tempVideoFile);

            videoAsset = videoAssetRepository.findById(videoAssetId).orElseThrow();
            videoAsset.markProcessingCompleted();
            videoAsset.markUploadCompleted(null);
            videoAsset.setUploadedParts(1);
            videoAssetRepository.save(videoAsset);

            java.util.Optional<Content> contentOpt = contentRepository.findById(videoAsset.getContent().getId());
            if (contentOpt.isPresent()) {
                Content content = contentOpt.get();
                content.setPublishStatus(PublishStatus.PUBLISHED);
                content.setDurationSeconds(durationSeconds);
                contentRepository.save(content);
            }

            log.info("HLS + sprites completed for videoAssetId={}", videoAssetId);
        } catch (Exception e) {
            log.error("HLS/sprite processing failed for videoAssetId={}: {}", videoAssetId, e.getMessage(), e);
            try {
                VideoAsset va = videoAssetRepository.findById(videoAssetId).orElse(null);
                if (va != null) {
                    va.markProcessingFailed(e.getMessage());
                    va.markUploadFailed(e.getMessage());
                    videoAssetRepository.save(va);
                }
            } catch (Exception inner) {
                log.error("Failed to update status after processing failure for videoAssetId={}: {}",
                        videoAssetId, inner.getMessage());
            }
        }
    }

    private Path copyToTempFile(MultipartFile videoFile) throws IOException {
        Path tempFile = Files.createTempFile("streamflow-upload-", ".tmp");
        videoFile.transferTo(tempFile.toFile());
        return tempFile;
    }

    private void deleteTempFileSilently(Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }

    /**
     * Aborts a multipart upload that is no longer needed (e.g., for cleanup).
     *
     * @param videoAssetId the ID of the video asset
     * @throws ResourceNotFoundException if video asset not found
     */
    @Transactional
    public void abortUpload(UUID videoAssetId) {
        VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));

        abortExistingUpload(videoAsset);
    }

    /**
     * Gets the upload status for a video asset.
     *
     * @param videoAssetId the ID of the video asset
     * @return the VideoAsset with current status
     * @throws ResourceNotFoundException if video asset not found
     */
    @Transactional(readOnly = true)
    public VideoAsset getUploadStatus(UUID videoAssetId) {
        return videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
    }

    /**
     * Gets the video asset for a content.
     *
     * @param contentId the ID of the content
     * @return the VideoAsset if exists, empty optional otherwise
     */
    @Transactional(readOnly = true)
    public java.util.Optional<VideoAsset> getVideoAssetByContentId(UUID contentId) {
        return videoAssetRepository.findByContentId(contentId);
    }

    /**
     * Retries a failed upload.
     *
     * NOT @Transactional: delegates to uploadVideoMovie() which manages its own
     * DB saves outside of a long-running transaction.
     *
     * @param videoAssetId the ID of the video asset with failed upload
     * @param videoFile    the video file to upload
     * @return the updated VideoAsset
     * @throws ResourceNotFoundException if video asset not found
     * @throws BadRequestException       if upload is not in FAILED status
     */
    public VideoAsset retryFailedUpload(UUID videoAssetId, MultipartFile videoFile) {
        VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));

        if (videoAsset.getUploadStatus() != UploadStatus.FAILED &&
                videoAsset.getUploadStatus() != UploadStatus.CANCELLED) {
            throw new BadRequestException(
                    "Can only retry uploads in FAILED or CANCELLED status. Current status: " +
                            videoAsset.getUploadStatus());
        }

        UUID contentId = videoAsset.getContent().getId();
        return uploadVideoMovie(contentId, videoFile);
    }
}
