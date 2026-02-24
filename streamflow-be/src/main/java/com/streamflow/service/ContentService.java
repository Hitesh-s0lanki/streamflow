package com.streamflow.service;

import com.streamflow.dto.CreateContentRequest;
import com.streamflow.dto.VideoUploadResponse;
import com.streamflow.entity.Content;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.streamflow.dto.ContentDetailResponse;
import com.streamflow.dto.ContentSummaryResponse;

/**
 * Service for content management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final S3StorageService s3StorageService;
    private final VideoUploadService videoUploadService;

    /**
     * Creates new content in DRAFT status.
     *
     * @param request the create request
     * @return ResponseEntity with 201 and body on success, 400 with message on bad
     *         request,
     *         or 500 on unexpected error (errors are logged in this service)
     */
    @Transactional
    public ResponseEntity<?> createContent(CreateContentRequest request) {
        try {
            Content content = new Content();
            content.setTitle(request.getTitle());
            content.setDescription(request.getDescription());
            content.setContentType(request.getContentType());
            content.setReleaseYear(request.getReleaseYear());
            content.setRating(request.getRating());
            content.setDurationSeconds(request.getDurationSeconds());
            content.setPublishStatus(PublishStatus.DRAFT);

            content = contentRepository.save(content);
            return ResponseEntity.status(HttpStatus.CREATED).body(content);
        } catch (DataIntegrityViolationException e) {
            String message = "Failed to create content: data integrity violation";
            log.error("Data integrity violation while creating content: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad request", "message", message));
        } catch (Exception e) {
            log.error("Unexpected error while creating content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to create content"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getAllContent() {
        try {
            List<ContentSummaryResponse> summaries = contentRepository.findAll().stream()
                    .map(this::toSummary)
                    .toList();
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            log.error("Unexpected error while fetching all content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to fetch content"));
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getContentById(UUID contentId) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
            return ResponseEntity.ok(toDetail(content));
        } catch (ResourceNotFoundException e) {
            log.warn("Content not found: contentId={}", contentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while fetching content: contentId={}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to fetch content"));
        }
    }

    private ContentSummaryResponse toSummary(Content content) {
        return ContentSummaryResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .contentType(content.getContentType())
                .thumbnailUrl(content.getThumbnailUrl())
                .publishStatus(content.getPublishStatus())
                .releaseYear(content.getReleaseYear())
                .createdAt(content.getCreatedAt())
                .build();
    }

    private ContentDetailResponse toDetail(Content content) {
        return ContentDetailResponse.builder()
                .id(content.getId())
                .title(content.getTitle())
                .description(content.getDescription())
                .contentType(content.getContentType())
                .releaseYear(content.getReleaseYear())
                .rating(content.getRating())
                .posterUrl(content.getPosterUrl())
                .thumbnailUrl(content.getThumbnailUrl())
                .publishStatus(content.getPublishStatus())
                .durationSeconds(content.getDurationSeconds())
                .createdAt(content.getCreatedAt())
                .updatedAt(content.getUpdatedAt())
                .build();
    }

    /**
     * Uploads poster and thumbnail for content.
     *
     * NOT @Transactional: S3 image uploads are network I/O and must not hold a
     * database transaction open (Neon serverless Postgres kills idle-in-transaction
     * connections). Each repository call uses its own implicit transaction.
     *
     * @param contentId the content ID
     * @param poster    the poster image file
     * @param thumbnail the thumbnail image file
     * @return ResponseEntity with 200 and updated content on success, 404 with
     *         message if content
     *         not found, or 500 on unexpected error (errors are logged in this
     *         service)
     */
    public ResponseEntity<?> uploadAssets(UUID contentId, MultipartFile poster, MultipartFile thumbnail) {
        try {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

            String posterKey = s3StorageService.uploadImage(poster);
            String thumbnailKey = s3StorageService.uploadImage(thumbnail);

            content.setPosterUrl(posterKey);
            content.setThumbnailUrl(thumbnailKey);

            content = contentRepository.save(content);
            return ResponseEntity.ok(content);
        } catch (ResourceNotFoundException e) {
            log.warn("Content not found for asset upload: contentId={}, message={}", contentId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error while uploading assets for content: contentId={}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error", "message", "Failed to upload assets"));
        }
    }

    /**
     * Validates the upload request synchronously, then kicks off the S3 upload
     * in the background. Returns 200 immediately so the caller can poll
     * {@code GET /api/content/{id}/video/status} for progress.
     *
     * @param contentId the content ID (must be a MOVIE type)
     * @param videoFile the video file to upload
     * @return 200 with VideoUploadResponse on success, or appropriate error status
     */
    public ResponseEntity<?> uploadVideo(UUID contentId, MultipartFile videoFile) {
        try {
            log.info("Initiating video upload for content: {}, filename: {}, size: {} bytes",
                    contentId, videoFile.getOriginalFilename(), videoFile.getSize());

            VideoAsset videoAsset = videoUploadService.validateAndPrepareUpload(contentId, videoFile);

            Path tempFile = Files.createTempFile("streamflow-upload-", ".tmp");
            videoFile.transferTo(tempFile.toFile());

            videoUploadService.uploadVideoMovieAsync(videoAsset.getId(), tempFile);

            VideoUploadResponse response = buildVideoUploadResponse(videoAsset,
                    "Video upload started. Poll GET /api/content/" + contentId + "/video/status for progress.");
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.warn("Content not found for video upload: contentId={}", contentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Not found", "message", e.getMessage()));
        } catch (BadRequestException e) {
            log.warn("Invalid video upload request: contentId={}, reason={}", contentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Bad request", "message", e.getMessage()));
        } catch (IOException e) {
            log.error("Failed to prepare video file for upload: contentId={}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error",
                            "message", "Failed to prepare video file for upload"));
        } catch (Exception e) {
            log.error("Unexpected error while initiating video upload: contentId={}", contentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error",
                            "message", "Failed to initiate video upload"));
        }
    }

    /**
     * Gets the current upload status for a content's video.
     *
     * @param contentId the content ID
     * @return VideoUploadResponse with current status, or null if no video asset
     *         exists
     * @throws ResourceNotFoundException if content not found
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getVideoUploadStatus(UUID contentId) {
        try {
            // Verify content exists
            if (!contentRepository.existsById(contentId)) {
                throw new ResourceNotFoundException("Content", contentId);
            }

            Optional<VideoAsset> videoAssetOpt = videoUploadService.getVideoAssetByContentId(contentId);

            if (videoAssetOpt.isEmpty()) {
                return null;
            }

            VideoAsset videoAsset = videoAssetOpt.get();
            String message = switch (videoAsset.getUploadStatus()) {
                case PENDING -> "Upload pending";
                case UPLOADING -> "Upload in progress";
                case MULTIPART_INITIATED -> "Multipart upload initiated, uploading parts...";
                case COMPLETED -> "Video uploaded successfully";
                case FAILED -> "Upload failed: " + videoAsset.getUploadErrorMessage();
                case CANCELLED -> "Upload was cancelled";
            };

            return ResponseEntity.ok(buildVideoUploadResponse(videoAsset, message));
        } catch (ResourceNotFoundException e) {
            log.error("Content not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error while getting video upload status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Retries a failed video upload.
     *
     * @param contentId the content ID
     * @param videoFile the video file to upload
     * @return VideoUploadResponse with upload details
     * @throws ResourceNotFoundException if content or video asset not found
     * @throws BadRequestException       if upload is not in a retryable state
     */
    public ResponseEntity<?> retryVideoUpload(UUID contentId, MultipartFile videoFile) {
        try {
            Optional<VideoAsset> videoAssetOpt = videoUploadService.getVideoAssetByContentId(contentId);

            if (videoAssetOpt.isEmpty()) {
                throw new ResourceNotFoundException("VideoAsset for Content", contentId);
            }

            VideoAsset videoAsset = videoUploadService.retryFailedUpload(videoAssetOpt.get().getId(), videoFile);
            return ResponseEntity.ok(buildVideoUploadResponse(videoAsset, "Video upload retry completed"));
        } catch (ResourceNotFoundException e) {
            log.error("Content not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error while retrying video upload: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Aborts an in-progress video upload.
     *
     * @param contentId the content ID
     * @throws ResourceNotFoundException if content or video asset not found
     */
    public ResponseEntity<?> abortVideoUpload(UUID contentId) {
        try {
            Optional<VideoAsset> videoAssetOpt = videoUploadService.getVideoAssetByContentId(contentId);

            if (videoAssetOpt.isEmpty()) {
                throw new ResourceNotFoundException("VideoAsset for Content", contentId);
            }

            videoUploadService.abortUpload(videoAssetOpt.get().getId());
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            log.error("Content not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error while aborting video upload: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Builds a VideoUploadResponse from a VideoAsset.
     */
    private VideoUploadResponse buildVideoUploadResponse(VideoAsset videoAsset, String message) {
        return VideoUploadResponse.builder()
                .contentId(videoAsset.getContent().getId())
                .videoAssetId(videoAsset.getId())
                .uploadStatus(videoAsset.getUploadStatus())
                .originalFilename(videoAsset.getOriginalFilename())
                .fileSizeBytes(videoAsset.getFileSizeBytes())
                .rawS3Key(videoAsset.getRawS3Key())
                .uploadStartedAt(videoAsset.getUploadStartedAt())
                .uploadCompletedAt(videoAsset.getUploadCompletedAt())
                .errorMessage(videoAsset.getUploadErrorMessage())
                .totalParts(videoAsset.getTotalParts())
                .uploadedParts(videoAsset.getUploadedParts())
                .progressPercent(calculateProgressPercent(videoAsset))
                .message(message)
                .build();
    }

    private Integer calculateProgressPercent(VideoAsset videoAsset) {
        return switch (videoAsset.getUploadStatus()) {
            case COMPLETED -> 100;
            case FAILED, CANCELLED -> null;
            case PENDING -> 0;
            case UPLOADING, MULTIPART_INITIATED -> {
                Integer total = videoAsset.getTotalParts();
                Integer uploaded = videoAsset.getUploadedParts();
                if (total != null && total > 0 && uploaded != null) {
                    yield Math.min(99, Math.round((float) uploaded / total * 100));
                }
                yield 0;
            }
        };
    }
}
