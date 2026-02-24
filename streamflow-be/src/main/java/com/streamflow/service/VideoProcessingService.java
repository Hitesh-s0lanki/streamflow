package com.streamflow.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.streamflow.dto.VideoProcessingResponse;
import com.streamflow.entity.Content;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.ProcessingStatus;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.VideoAssetRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles sprite-sheet-only processing for already-uploaded video.
 * HLS transcoding runs in the upload API (VideoUploadService) from the same
 * temp file; this service only generates sprites, using the raw video from S3.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoProcessingService {

    private final ContentRepository contentRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final SpriteSheetService spriteSheetService;
    private final S3StorageService s3StorageService;

    /** Self-injection so @Async calls go through the Spring proxy. */
    @Lazy
    @Autowired
    private VideoProcessingService self;

    /**
     * Triggers sprite-sheet generation only. Video is downloaded from S3 (raw)
     * and no video file is required in the request. Returns 200 immediately;
     * poll GET /api/content/{id}/video/process/status for progress.
     */
    @Transactional
    public ResponseEntity<?> processMovieVideo(UUID contentId) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

            VideoAsset videoAsset = content.getVideoAsset();
            if (videoAsset == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Bad request",
                                "message", "No video asset found for this content. Upload a video first."));
            }

            if (!StringUtils.hasText(videoAsset.getRawS3Key())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Bad request",
                                "message", "Video must be uploaded first (raw S3 key missing)."));
            }

            if (videoAsset.getProcessingStatus() == ProcessingStatus.PROCESSING
                    || videoAsset.getProcessingStatus() == ProcessingStatus.QUEUED) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Conflict", "message", "Video processing is already in progress"));
            }

            videoAsset.markProcessingQueued();
            videoAssetRepository.save(videoAsset);

            self.processAsyncSpriteOnly(videoAsset.getId(), contentId);

            return ResponseEntity.ok()
                    .body(buildResponse(videoAsset,
                            "Sprite generation started. Poll GET /api/content/" + contentId + "/video/process/status for progress."));
        } catch (ResourceNotFoundException e) {
            log.warn("Content not found for video processing: contentId={}", contentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error initiating sprite processing for content {}: {}", contentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to initiate sprite processing"));
        }
    }

    /**
     * Returns the current processing status for a content's video.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getProcessingStatus(UUID contentId) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

            VideoAsset videoAsset = content.getVideoAsset();
            if (videoAsset == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Not found", "message", "No video asset found for this content"));
            }

            String message = switch (videoAsset.getProcessingStatus()) {
                case NONE -> "No processing has been triggered";
                case QUEUED -> "Processing is queued and will start shortly";
                case PROCESSING -> "Sprite sheet generation in progress";
                case COMPLETED -> "Video processing completed successfully";
                case FAILED -> "Processing failed: " + videoAsset.getProcessingErrorMessage();
            };

            return ResponseEntity.ok(buildResponse(videoAsset, message));
        } catch (ResourceNotFoundException e) {
            log.error("Content not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting processing status for content {}: {}", contentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to get processing status"));
        }
    }

    /**
     * Runs sprite-sheet generation only. Downloads the raw video from S3 to a
     * temp file, generates sprites, then deletes the temp file.
     */
    @Async
    public void processAsyncSpriteOnly(UUID videoAssetId, UUID contentId) {
        Path tempFile = null;
        try {
            VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId)
                    .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));

            String rawS3Key = videoAsset.getRawS3Key();
            if (!StringUtils.hasText(rawS3Key)) {
                videoAsset.markProcessingFailed("Raw S3 key missing");
                videoAssetRepository.save(videoAsset);
                return;
            }

            videoAsset.markProcessingStarted();
            videoAssetRepository.save(videoAsset);

            log.info("Starting sprite-only processing for videoAsset={}, content={}", videoAssetId, contentId);

            tempFile = s3StorageService.downloadToTempFile(rawS3Key);
            spriteSheetService.processVideoAndGenerateSprites(videoAsset, tempFile);

            videoAsset = videoAssetRepository.findById(videoAssetId).orElseThrow();
            videoAsset.markProcessingCompleted();
            videoAssetRepository.save(videoAsset);

            Optional<Content> contentOpt = contentRepository.findById(contentId);
            if (contentOpt.isPresent()) {
                Content content = contentOpt.get();
                content.setPublishStatus(PublishStatus.PUBLISHED);
                Integer duration = videoAsset.getDurationSeconds();
                if (duration != null && duration > 0) {
                    content.setDurationSeconds(duration);
                }
                contentRepository.save(content);
            }

            log.info("Sprite processing completed for videoAsset={}, content={}", videoAssetId, contentId);
        } catch (Exception e) {
            log.error("Sprite processing failed for videoAsset={}, content={}: {}", videoAssetId, contentId, e.getMessage(), e);
            try {
                VideoAsset va = videoAssetRepository.findById(videoAssetId).orElse(null);
                if (va != null) {
                    va.markProcessingFailed(e.getMessage());
                    videoAssetRepository.save(va);
                }
            } catch (Exception inner) {
                log.error("Failed to update processing status to FAILED for videoAsset={}: {}", videoAssetId, inner.getMessage());
            }
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    private VideoProcessingResponse buildResponse(VideoAsset videoAsset, String message) {
        return VideoProcessingResponse.builder()
                .contentId(videoAsset.getContent() != null ? videoAsset.getContent().getId() : null)
                .videoAssetId(videoAsset.getId())
                .processingStatus(videoAsset.getProcessingStatus())
                .processingStartedAt(videoAsset.getProcessingStartedAt())
                .processingCompletedAt(videoAsset.getProcessingCompletedAt())
                .errorMessage(videoAsset.getProcessingErrorMessage())
                .message(message)
                .build();
    }
}
