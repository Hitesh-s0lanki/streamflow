package com.streamflow.service;

import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngestionJobService {

    private final IngestionJobRepository ingestionJobRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<IngestionJob> findById(UUID id) {
        return ingestionJobRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<IngestionJob> findByVideoAssetId(UUID videoAssetId, Pageable pageable) {
        return ingestionJobRepository.findByVideoAssetIdOrderByCreatedAtDesc(videoAssetId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<IngestionJob> findLatestActiveByVideoAssetId(UUID videoAssetId) {
        return ingestionJobRepository.findFirstByVideoAssetIdAndJobStatusInOrderByCreatedAtDesc(
                videoAssetId, List.of(IngestionStatus.PENDING, IngestionStatus.UPLOADING, IngestionStatus.UPLOADED,
                        IngestionStatus.PROCESSING, IngestionStatus.TRANSCODED, IngestionStatus.SPRITES_GENERATED));
    }

    @Transactional(readOnly = true)
    public List<IngestionJob> findByStatus(IngestionStatus jobStatus, Pageable pageable) {
        return ingestionJobRepository.findByJobStatusOrderByCreatedAtDesc(jobStatus, pageable);
    }

    @Transactional
    public IngestionJob create(UUID videoAssetId, String rawS3Key) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        IngestionJob job = new IngestionJob();
        job.setVideoAsset(asset);
        job.setJobStatus(IngestionStatus.PENDING);
        job.setRawS3Key(rawS3Key);
        return ingestionJobRepository.save(job);
    }

    @Transactional
    public IngestionJob updateStatus(UUID id, IngestionStatus status, String errorMessage) {
        IngestionJob job = ingestionJobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("IngestionJob not found: " + id));
        job.setJobStatus(status);
        job.setErrorMessage(errorMessage);
        if (status == IngestionStatus.READY || status == IngestionStatus.FAILED) {
            job.setProcessedAt(Instant.now());
        }
        return ingestionJobRepository.save(job);
    }

    @Transactional
    public void deleteById(UUID id) {
        ingestionJobRepository.deleteById(id);
    }
}
