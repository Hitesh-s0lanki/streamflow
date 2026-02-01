package com.streamflow.service;

import com.streamflow.config.AwsProperties;
import com.streamflow.dto.admin.*;
import com.streamflow.entity.Content;
import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates admin upload and ingestion: create content/video assets,
 * generate presigned URLs, create ingestion jobs, and publish Kafka events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminIngestionService {

    private final ContentService contentService;
    private final VideoAssetService videoAssetService;
    private final ContentRepository contentRepository;
    private final S3MediaService s3MediaService;
    private final AwsProperties awsProperties;
    private final IngestionJobService ingestionJobService;
    private final Optional<MediaIngestionProducer> mediaIngestionProducer;

    @Transactional
    public ContentResponse createContent(CreateContentRequest request) {
        Content content = new Content();
        content.setTitle(request.getTitle());
        content.setDescription(request.getDescription());
        content.setContentType(request.getContentType());
        content.setReleaseYear(request.getReleaseYear());
        content.setRating(request.getRating());
        content.setPosterUrl(request.getPosterUrl());
        content.setThumbnailUrl(request.getThumbnailUrl());
        content.setPublishStatus(PublishStatus.DRAFT);
        content.setDurationSeconds(request.getDurationSeconds());
        content = contentService.save(content);
        return toContentResponse(content);
    }

    @Transactional
    public VideoAssetResponse createVideoAsset(CreateVideoAssetRequest request) {
        Content content = contentRepository.findById(request.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("Content not found: " + request.getContentId()));

        if (content.getContentType() == ContentType.MOVIE) {
            if (content.getVideoAsset() != null) {
                throw new IllegalArgumentException("Movie already has a video asset");
            }
            VideoAsset asset = new VideoAsset();
            asset.setContent(content);
            asset.setDurationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0);
            asset.setDrmEnabled(false);
            asset = videoAssetService.save(asset);
            content.setVideoAsset(asset);
            contentRepository.save(content);
            return toVideoAssetResponse(asset);
        }

        // SERIES: create placeholder asset (contentId only); episode attachment is via
        // EpisodeService
        VideoAsset asset = new VideoAsset();
        asset.setContent(content);
        asset.setDurationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0);
        asset.setDrmEnabled(false);
        asset = videoAssetService.save(asset);
        return toVideoAssetResponse(asset);
    }

    public PresignedUploadResponse generatePresignedUploadUrl(UUID videoAssetId, String contentType) {
        videoAssetService.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));

        var result = s3MediaService.generatePresignedUploadUrl(videoAssetId,
                contentType != null ? contentType : "application/octet-stream");
        return PresignedUploadResponse.builder()
                .uploadUrl(result.uploadUrl())
                .s3Key(result.s3Key())
                .expiresInMinutes(awsProperties.getPresignedUploadExpiryMinutes())
                .build();
    }

    @Transactional
    public TriggerIngestResponse triggerIngest(UUID videoAssetId) {
        VideoAsset asset = videoAssetService.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));

        String rawS3Key = s3MediaService.buildRawVideoKey(videoAssetId);
        IngestionJob job = ingestionJobService.create(videoAssetId, rawS3Key);
        UUID jobId = job.getId();

        mediaIngestionProducer.ifPresent(producer -> {
            try {
                producer.sendIngestionRequest(jobId, videoAssetId, rawS3Key, "video/mp4",
                        Boolean.TRUE.equals(asset.getDrmEnabled()));
            } catch (Exception e) {
                log.warn("Failed to publish ingestion event to Kafka; worker may poll DB: {}", e.getMessage());
            }
        });

        return TriggerIngestResponse.builder()
                .jobId(jobId)
                .videoAssetId(videoAssetId)
                .status(IngestionStatus.PENDING)
                .rawS3Key(rawS3Key)
                .build();
    }

    public IngestionJobResponse getIngestionJob(UUID jobId) {
        var job = ingestionJobService.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("IngestionJob not found: " + jobId));
        return toIngestionJobResponse(job);
    }

    private static ContentResponse toContentResponse(Content c) {
        return ContentResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .contentType(c.getContentType())
                .releaseYear(c.getReleaseYear())
                .rating(c.getRating())
                .posterUrl(c.getPosterUrl())
                .thumbnailUrl(c.getThumbnailUrl())
                .publishStatus(c.getPublishStatus())
                .durationSeconds(c.getDurationSeconds())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private static VideoAssetResponse toVideoAssetResponse(VideoAsset v) {
        return VideoAssetResponse.builder()
                .id(v.getId())
                .contentId(v.getContent() != null ? v.getContent().getId() : null)
                .episodeId(v.getEpisode() != null ? v.getEpisode().getId() : null)
                .durationSeconds(v.getDurationSeconds())
                .manifestUrl(v.getManifestUrl())
                .drmEnabled(v.getDrmEnabled())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private static IngestionJobResponse toIngestionJobResponse(IngestionJob j) {
        return IngestionJobResponse.builder()
                .id(j.getId())
                .videoAssetId(j.getVideoAsset().getId())
                .jobStatus(j.getJobStatus())
                .rawS3Key(j.getRawS3Key())
                .errorMessage(j.getErrorMessage())
                .processedAt(j.getProcessedAt())
                .createdAt(j.getCreatedAt())
                .build();
    }
}
