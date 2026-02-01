package com.streamflow.worker.service;

import com.streamflow.entity.*;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.repository.*;
import com.streamflow.worker.config.WorkerProperties;
import com.streamflow.worker.dto.IngestionEventPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates the full ingestion pipeline: download → transcode → upload
 * packaged → update DB → sprites → upload sprites → persist sprites → READY.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionPipelineService {

    private final WorkerProperties workerProperties;
    private final IngestionJobRepository ingestionJobRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final VideoVariantRepository videoVariantRepository;
    private final SpriteSheetRepository spriteSheetRepository;
    private final SpriteFrameMetadataRepository spriteFrameMetadataRepository;
    private final S3DownloadService s3DownloadService;
    private final S3UploadService s3UploadService;
    private final FFmpegTranscodeService ffmpegTranscodeService;
    private final SpriteGeneratorService spriteGeneratorService;
    private final Optional<MediaCompletionProducer> completionProducer;

    private static final List<String> RESOLUTIONS = List.of("240", "480", "720", "1080");
    private static final List<Integer> SORT_ORDER = List.of(1, 2, 3, 4);

    @Transactional
    public void runPipeline(IngestionEventPayload payload) {
        UUID jobId = payload.getJobId();
        UUID videoAssetId = payload.getVideoAssetId();
        String rawS3Key = payload.getRawS3Key();
        String tempDir = workerProperties.getTempDir();
        Path workDir = Path.of(tempDir, videoAssetId.toString());
        Path packagedDir = workDir.resolve("packaged");
        Path spritesDir = workDir.resolve("sprites");

        IngestionJob job = ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("IngestionJob not found: " + jobId));
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));

        try {
            // 1) PROCESSING
            updateJobStatus(job, IngestionStatus.PROCESSING, null);

            // 2) Download raw to temp
            Path localInput = s3DownloadService.downloadToTemp(rawS3Key, videoAssetId, tempDir);

            // 3) Duration for sprites
            int durationSeconds = spriteGeneratorService.getDurationSeconds(localInput);
            asset.setDurationSeconds(durationSeconds);
            videoAssetRepository.save(asset);

            // 4) Transcode to HLS
            ffmpegTranscodeService.transcodeToHls(localInput, packagedDir, videoAssetId);

            // 5) TRANSCODED
            updateJobStatus(job, IngestionStatus.TRANSCODED, null);

            // 6) Upload packaged to S3
            s3UploadService.uploadPackagedDirectory(videoAssetId, packagedDir);

            String manifestKey = "packaged/" + videoAssetId + "/master.m3u8";
            String manifestUrl = "s3://" + s3UploadService.getPackagedBucket() + "/" + manifestKey;
            asset.setManifestUrl(manifestUrl);
            videoAssetRepository.save(asset);

            // 7) Create VideoVariant records
            for (int i = 0; i < RESOLUTIONS.size(); i++) {
                String res = RESOLUTIONS.get(i);
                if (videoVariantRepository.existsByVideoAssetIdAndResolution(videoAssetId, res)) {
                    continue;
                }
                VideoVariant variant = new VideoVariant();
                variant.setVideoAsset(asset);
                variant.setResolution(res + "p");
                variant.setSortOrder(SORT_ORDER.get(i));
                variant.setCodec("h264");
                variant.setSegmentPath("packaged/" + videoAssetId + "/" + res + "/playlist.m3u8");
                videoVariantRepository.save(variant);
            }

            // 8) Generate sprites
            List<SpriteGeneratorService.SpriteSheetResult> spriteResults = spriteGeneratorService.generateSprites(
                    localInput, spritesDir, durationSeconds);

            // 9) SPRITES_GENERATED
            updateJobStatus(job, IngestionStatus.SPRITES_GENERATED, null);

            // 10) Upload sprites to S3 and persist SpriteSheet + SpriteFrameMetadata
            for (int i = 0; i < spriteResults.size(); i++) {
                SpriteGeneratorService.SpriteSheetResult sr = spriteResults.get(i);
                String filename = "sprite_" + String.format("%03d", i + 1) + ".jpg";
                String spriteUrl = s3UploadService.uploadSprite(videoAssetId, filename, sr.localPath()).toString();

                SpriteSheet sheet = new SpriteSheet();
                sheet.setVideoAsset(asset);
                sheet.setSpriteUrl(spriteUrl);
                sheet.setStartTimeSeconds(sr.startTimeSeconds());
                sheet.setEndTimeSeconds(sr.endTimeSeconds());
                sheet.setColumns(sr.columns());
                sheet.setRows(sr.rows());
                sheet.setThumbnailWidth(sr.thumbnailWidth());
                sheet.setThumbnailHeight(sr.thumbnailHeight());
                sheet.setIntervalSeconds(sr.intervalSeconds());
                sheet = spriteSheetRepository.save(sheet);

                for (SpriteGeneratorService.SpriteSheetResult.FrameMeta fm : sr.frameMetadata()) {
                    SpriteFrameMetadata meta = new SpriteFrameMetadata();
                    meta.setSpriteSheet(sheet);
                    meta.setFrameIndex(fm.frameIndex());
                    meta.setTimeOffsetSeconds(fm.timeOffsetSeconds());
                    meta.setXPosition(fm.xPosition());
                    meta.setYPosition(fm.yPosition());
                    meta.setWidth(fm.width());
                    meta.setHeight(fm.height());
                    spriteFrameMetadataRepository.save(meta);
                }
            }

            // 11) READY
            updateJobStatus(job, IngestionStatus.READY, null);
            job.setProcessedAt(Instant.now());
            ingestionJobRepository.save(job);

            completionProducer.ifPresent(p -> p.sendIngestionCompleted(jobId, videoAssetId));
            log.info("Ingestion completed: jobId={}, videoAssetId={}", jobId, videoAssetId);

        } catch (Exception e) {
            log.error("Ingestion failed: jobId={}, videoAssetId={}", jobId, videoAssetId, e);
            updateJobStatus(job, IngestionStatus.FAILED, e.getMessage());
            job.setProcessedAt(Instant.now());
            ingestionJobRepository.save(job);
        } finally {
            cleanupDir(workDir);
        }
    }

    private void updateJobStatus(IngestionJob job, IngestionStatus status, String errorMessage) {
        job.setJobStatus(status);
        job.setErrorMessage(errorMessage);
        if (status == IngestionStatus.READY || status == IngestionStatus.FAILED) {
            job.setProcessedAt(Instant.now());
        }
        ingestionJobRepository.save(job);
    }

    private static void cleanupDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Cleanup failed for {}: {}", dir, e.getMessage());
        }
    }
}
