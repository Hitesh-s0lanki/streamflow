package com.streamflow.service;

import com.streamflow.dto.ConfirmUploadRequest;
import com.streamflow.dto.IngestionStatusResponse;
import com.streamflow.dto.PagedResponse;
import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.kafka.producer.KafkaProducerService;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Handles upload confirmation, ingestion status, and admin status overrides.
 * Creates IngestionJob on confirm and emits Kafka ingestion event.
 */
@Service
public class IngestionService {

    private final VideoAssetRepository videoAssetRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final KafkaProducerService kafkaProducerService;

    public IngestionService(VideoAssetRepository videoAssetRepository,
            IngestionJobRepository ingestionJobRepository,
            KafkaProducerService kafkaProducerService) {
        this.videoAssetRepository = videoAssetRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Transactional
    public IngestionStatusResponse confirmUpload(UUID videoAssetId, ConfirmUploadRequest request) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        String rawS3Key = request.getRawS3Key().trim();
        String contentType = request.getContentType() != null && !request.getContentType().isBlank()
                ? request.getContentType().trim()
                : "application/octet-stream";

        IngestionJob job = new IngestionJob();
        job.setVideoAsset(asset);
        job.setJobStatus(IngestionStatus.UPLOADED);
        job.setRawS3Key(rawS3Key);
        job.setContentType(contentType);
        job = ingestionJobRepository.save(job);

        UUID movieId = asset.getContent() != null && asset.getEpisode() == null ? asset.getContent().getId() : null;
        UUID episodeId = asset.getEpisode() != null ? asset.getEpisode().getId() : null;
        kafkaProducerService.sendIngestionEvent(asset.getId(), rawS3Key, contentType, movieId, episodeId);

        return toStatusResponse(job);
    }

    @Transactional(readOnly = true)
    public IngestionStatusResponse getIngestionStatus(UUID videoAssetId) {
        if (!videoAssetRepository.existsById(videoAssetId)) {
            throw new ResourceNotFoundException("VideoAsset", videoAssetId);
        }
        return ingestionJobRepository.findFirstByVideoAssetIdOrderByCreatedAtDesc(videoAssetId)
                .map(this::toStatusResponse)
                .orElse(IngestionStatusResponse.builder()
                        .jobId(null)
                        .videoAssetId(videoAssetId)
                        .jobStatus(IngestionStatus.PENDING)
                        .processedAt(null)
                        .errorMessage(null)
                        .createdAt(null)
                        .build());
    }

    /** Admin: list ingestion jobs with filters. Order: createdAt DESC. Paginated. */
    @Transactional(readOnly = true)
    public PagedResponse<IngestionStatusResponse> adminListJobs(IngestionStatus jobStatus,
            UUID videoAssetId, UUID contentId, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IngestionJob> p = ingestionJobRepository.findAdminList(
                jobStatus, videoAssetId, contentId, from, to, pageable);
        List<IngestionStatusResponse> items = p.getContent().stream()
                .map(this::toStatusResponse)
                .toList();
        return PagedResponse.<IngestionStatusResponse>builder()
                .content(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    /** Admin: get ingestion job detail (includes rawS3Key, errorMessage, processedAt). */
    @Transactional(readOnly = true)
    public IngestionStatusResponse adminGetJobDetail(UUID jobId) {
        IngestionJob job = ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("IngestionJob", jobId));
        IngestionStatusResponse r = toStatusResponse(job);
        r.setRawS3Key(job.getRawS3Key());
        return r;
    }

    @Transactional
    public IngestionStatusResponse adminOverrideStatus(UUID jobId, IngestionStatus newStatus) {
        IngestionJob job = ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("IngestionJob", jobId));
        IngestionStatus current = job.getJobStatus();

        if (newStatus == IngestionStatus.FAILED) {
            job.setJobStatus(IngestionStatus.FAILED);
            job.setProcessedAt(Instant.now());
        } else if (current == IngestionStatus.PROCESSING && newStatus == IngestionStatus.TRANSCODED) {
            job.setJobStatus(IngestionStatus.TRANSCODED);
        } else if (current == IngestionStatus.TRANSCODED && newStatus == IngestionStatus.SPRITES_GENERATED) {
            job.setJobStatus(IngestionStatus.SPRITES_GENERATED);
        } else if (current == IngestionStatus.SPRITES_GENERATED && newStatus == IngestionStatus.READY) {
            job.setJobStatus(IngestionStatus.READY);
            job.setProcessedAt(Instant.now());
        } else {
            throw new BadRequestException(
                    "Invalid transition from " + current + " to " + newStatus
                            + ". Allowed: PROCESSING→TRANSCODED, TRANSCODED→SPRITES_GENERATED, SPRITES_GENERATED→READY, Any→FAILED");
        }
        job = ingestionJobRepository.save(job);
        return toStatusResponse(job);
    }

    private IngestionStatusResponse toStatusResponse(IngestionJob j) {
        return IngestionStatusResponse.builder()
                .jobId(j.getId())
                .videoAssetId(j.getVideoAsset().getId())
                .jobStatus(j.getJobStatus())
                .processedAt(j.getProcessedAt())
                .errorMessage(j.getErrorMessage())
                .createdAt(j.getCreatedAt())
                .build();
    }
}
