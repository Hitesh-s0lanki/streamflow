package com.streamflow.service;

import com.streamflow.dto.*;
import com.streamflow.entity.*;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates video assets and issues presigned upload URLs. Enforces one
 * VideoAsset per Movie/Episode.
 * Phase 3: ABR variants, manifest, sprite sheets, and sprite frame metadata.
 */
@Service
public class VideoAssetService {

    private static final List<IngestionStatus> TRANSCODED_OR_LATER = List.of(
            IngestionStatus.TRANSCODED,
            IngestionStatus.SPRITES_GENERATED,
            IngestionStatus.READY);

    private final VideoAssetRepository videoAssetRepository;
    private final ContentRepository contentRepository;
    private final EpisodeRepository episodeRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final VideoVariantRepository videoVariantRepository;
    private final SpriteSheetRepository spriteSheetRepository;
    private final SpriteFrameMetadataRepository spriteFrameMetadataRepository;
    private final S3StorageService s3StorageService;

    @Value("${streamflow.ingestion.presigned-url-expiration-minutes:15}")
    private int presignedUrlExpirationMinutes = 15;

    public VideoAssetService(VideoAssetRepository videoAssetRepository,
            ContentRepository contentRepository,
            EpisodeRepository episodeRepository,
            IngestionJobRepository ingestionJobRepository,
            VideoVariantRepository videoVariantRepository,
            SpriteSheetRepository spriteSheetRepository,
            SpriteFrameMetadataRepository spriteFrameMetadataRepository,
            @Autowired(required = false) S3StorageService s3StorageService) {
        this.videoAssetRepository = videoAssetRepository;
        this.contentRepository = contentRepository;
        this.episodeRepository = episodeRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.videoVariantRepository = videoVariantRepository;
        this.spriteSheetRepository = spriteSheetRepository;
        this.spriteFrameMetadataRepository = spriteFrameMetadataRepository;
        this.s3StorageService = s3StorageService;
    }

    @Transactional
    public VideoAssetResponse createVideoAsset(CreateVideoAssetRequest request) {
        UUID contentId = request.getContentId();
        UUID episodeId = request.getEpisodeId();
        boolean hasContent = contentId != null;
        boolean hasEpisode = episodeId != null;

        if (hasContent == hasEpisode) {
            throw new BadRequestException("Exactly one of contentId (for MOVIE) or episodeId (for SERIES) must be set");
        }

        if (hasContent) {
            Content content = contentRepository.findById(contentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
            if (content.getContentType() != ContentType.MOVIE) {
                throw new BadRequestException("contentId must refer to a MOVIE");
            }
            if (videoAssetRepository.existsByContentId(contentId)) {
                throw new BadRequestException("This movie already has a VideoAsset");
            }
            VideoAsset asset = new VideoAsset();
            asset.setContent(content);
            asset.setEpisode(null);
            asset.setDurationSeconds(request.getDurationSeconds());
            asset.setDrmEnabled(false);
            asset = videoAssetRepository.save(asset);
            content.setVideoAsset(asset);
            return toResponse(asset);
        } else {
            Episode episode = episodeRepository.findById(episodeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Episode", episodeId));
            if (videoAssetRepository.existsByEpisodeId(episodeId)) {
                throw new BadRequestException("This episode already has a VideoAsset");
            }
            VideoAsset asset = new VideoAsset();
            asset.setContent(episode.getSeason().getContent());
            asset.setEpisode(episode);
            asset.setDurationSeconds(request.getDurationSeconds());
            asset.setDrmEnabled(false);
            asset = videoAssetRepository.save(asset);
            episode.setVideoAsset(asset);
            episodeRepository.save(episode);
            return toResponse(asset);
        }
    }

    @Transactional(readOnly = true)
    public UploadUrlResponse getUploadUrl(UUID videoAssetId) {
        videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        if (s3StorageService == null) {
            throw new BadRequestException(
                    "S3 upload is not configured. Set streamflow.aws.s3.enabled=true and configure bucket.");
        }
        String rawS3Key = s3StorageService.generateRawVideoKey(videoAssetId);
        return s3StorageService.generatePresignedPutUrl(rawS3Key, presignedUrlExpirationMinutes);
    }

    // --- Phase 3: Variants, manifest, sprites ---

    private void requireTranscodedOrLater(UUID videoAssetId) {
        IngestionJob latest = ingestionJobRepository.findFirstByVideoAssetIdOrderByCreatedAtDesc(videoAssetId)
                .orElseThrow(() -> new BadRequestException(
                        "No ingestion job found for VideoAsset; ingestion must reach at least TRANSCODED"));
        if (!TRANSCODED_OR_LATER.contains(latest.getJobStatus())) {
            throw new BadRequestException(
                    "Ingestion status must be TRANSCODED or later; current: " + latest.getJobStatus());
        }
    }

    private void requireNotReadyForVariantRegistration(UUID videoAssetId) {
        IngestionJob latest = ingestionJobRepository.findFirstByVideoAssetIdOrderByCreatedAtDesc(videoAssetId)
                .orElseThrow(() -> new BadRequestException("No ingestion job found for VideoAsset"));
        if (latest.getJobStatus() == IngestionStatus.READY) {
            throw new BadRequestException("No variant updates allowed once ingestion status is READY");
        }
    }

    @Transactional
    public VariantResponse addVariant(UUID videoAssetId, RegisterVariantRequest request) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        requireTranscodedOrLater(videoAssetId);
        requireNotReadyForVariantRegistration(videoAssetId);
        if (videoVariantRepository.existsByVideoAssetIdAndSortOrder(videoAssetId, request.getSortOrder())) {
            throw new BadRequestException("sortOrder must be unique per VideoAsset: " + request.getSortOrder());
        }
        VideoVariant variant = new VideoVariant();
        variant.setVideoAsset(asset);
        variant.setResolution(request.getResolution());
        variant.setBitrateKbps(request.getBitrateKbps());
        variant.setCodec(request.getCodec());
        variant.setSegmentPath(request.getSegmentPath());
        variant.setSortOrder(request.getSortOrder());
        variant = videoVariantRepository.save(variant);
        return toVariantResponse(variant);
    }

    @Transactional(readOnly = true)
    public List<VariantResponse> getVariants(UUID videoAssetId) {
        videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        return videoVariantRepository.findByVideoAssetIdOrderBySortOrderAsc(videoAssetId).stream()
                .map(this::toVariantResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void setManifest(UUID videoAssetId, RegisterManifestRequest request) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        if (videoVariantRepository.findByVideoAssetIdOrderBySortOrderAsc(videoAssetId).isEmpty()) {
            throw new BadRequestException("Variants must be registered before setting manifest");
        }
        if (asset.getManifestUrl() != null && !asset.getManifestUrl().isBlank()) {
            throw new BadRequestException("manifestUrl is immutable once set");
        }
        asset.setManifestUrl(request.getManifestUrl());
        videoAssetRepository.save(asset);
    }

    @Transactional
    public SpriteSheetResponse addSpriteSheet(UUID videoAssetId, RegisterSpriteRequest request) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        requireTranscodedOrLater(videoAssetId);
        if (request.getStartTimeSeconds() >= request.getEndTimeSeconds()) {
            throw new BadRequestException("startTimeSeconds must be less than endTimeSeconds");
        }
        if (request.getEndTimeSeconds() > asset.getDurationSeconds()) {
            throw new BadRequestException(
                    "Sprite coverage must not exceed video duration (" + asset.getDurationSeconds() + "s)");
        }
        List<SpriteSheet> existing = spriteSheetRepository.findByVideoAssetIdOrderByStartTimeSecondsAsc(videoAssetId);
        for (SpriteSheet s : existing) {
            if (rangesOverlap(s.getStartTimeSeconds(), s.getEndTimeSeconds(),
                    request.getStartTimeSeconds(), request.getEndTimeSeconds())) {
                throw new BadRequestException("Sprite time ranges must not overlap with existing sheet ["
                        + s.getStartTimeSeconds() + "," + s.getEndTimeSeconds() + "]");
            }
        }
        SpriteSheet sheet = new SpriteSheet();
        sheet.setVideoAsset(asset);
        sheet.setSpriteUrl(request.getSpriteUrl());
        sheet.setStartTimeSeconds(request.getStartTimeSeconds());
        sheet.setEndTimeSeconds(request.getEndTimeSeconds());
        sheet.setColumns(request.getColumns());
        sheet.setRows(request.getRows());
        sheet.setThumbnailWidth(request.getThumbnailWidth());
        sheet.setThumbnailHeight(request.getThumbnailHeight());
        sheet.setIntervalSeconds(request.getIntervalSeconds());
        sheet = spriteSheetRepository.save(sheet);
        return toSpriteSheetResponse(sheet);
    }

    @Transactional(readOnly = true)
    public List<SpriteSheetResponse> getSpritesForPlayback(UUID videoAssetId) {
        videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        return spriteSheetRepository.findByVideoAssetIdOrderByStartTimeSecondsAsc(videoAssetId).stream()
                .map(this::toSpriteSheetResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SpriteFrameResponse addSpriteFrame(UUID spriteSheetId, RegisterSpriteFrameRequest request) {
        SpriteSheet sheet = spriteSheetRepository.findById(spriteSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SpriteSheet", spriteSheetId));
        if (spriteFrameMetadataRepository.existsBySpriteSheetIdAndFrameIndex(spriteSheetId, request.getFrameIndex())) {
            throw new BadRequestException("frameIndex must be unique per SpriteSheet: " + request.getFrameIndex());
        }
        if (request.getTimeOffsetSeconds() < sheet.getStartTimeSeconds()
                || request.getTimeOffsetSeconds() > sheet.getEndTimeSeconds()) {
            throw new BadRequestException("timeOffsetSeconds must fall within sprite time range ["
                    + sheet.getStartTimeSeconds() + "," + sheet.getEndTimeSeconds() + "]");
        }
        SpriteFrameMetadata frame = new SpriteFrameMetadata();
        frame.setSpriteSheet(sheet);
        frame.setFrameIndex(request.getFrameIndex());
        frame.setTimeOffsetSeconds(request.getTimeOffsetSeconds());
        frame.setXPosition(request.getXPosition());
        frame.setYPosition(request.getYPosition());
        frame.setWidth(request.getWidth());
        frame.setHeight(request.getHeight());
        frame = spriteFrameMetadataRepository.save(frame);
        return toSpriteFrameResponse(frame);
    }

    @Transactional(readOnly = true)
    public List<SpriteFrameResponse> getSpriteFrames(UUID spriteSheetId) {
        spriteSheetRepository.findById(spriteSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SpriteSheet", spriteSheetId));
        return spriteFrameMetadataRepository.findBySpriteSheetIdOrderByFrameIndexAsc(spriteSheetId).stream()
                .map(this::toSpriteFrameResponse)
                .collect(Collectors.toList());
    }

    private static boolean rangesOverlap(int aStart, int aEnd, int bStart, int bEnd) {
        return aStart < bEnd && bStart < aEnd;
    }

    private VariantResponse toVariantResponse(VideoVariant v) {
        return VariantResponse.builder()
                .id(v.getId())
                .videoAssetId(v.getVideoAsset().getId())
                .resolution(v.getResolution())
                .bitrateKbps(v.getBitrateKbps())
                .codec(v.getCodec())
                .segmentPath(v.getSegmentPath())
                .sortOrder(v.getSortOrder())
                .build();
    }

    private SpriteSheetResponse toSpriteSheetResponse(SpriteSheet s) {
        return SpriteSheetResponse.builder()
                .id(s.getId())
                .spriteUrl(s.getSpriteUrl())
                .startTimeSeconds(s.getStartTimeSeconds())
                .endTimeSeconds(s.getEndTimeSeconds())
                .columns(s.getColumns())
                .rows(s.getRows())
                .thumbnailWidth(s.getThumbnailWidth())
                .thumbnailHeight(s.getThumbnailHeight())
                .intervalSeconds(s.getIntervalSeconds())
                .build();
    }

    private SpriteFrameResponse toSpriteFrameResponse(SpriteFrameMetadata f) {
        return SpriteFrameResponse.builder()
                .id(f.getId())
                .frameIndex(f.getFrameIndex())
                .timeOffsetSeconds(f.getTimeOffsetSeconds())
                .xPosition(f.getXPosition())
                .yPosition(f.getYPosition())
                .width(f.getWidth())
                .height(f.getHeight())
                .build();
    }

    private static VideoAssetResponse toResponse(VideoAsset a) {
        return VideoAssetResponse.builder()
                .id(a.getId())
                .contentId(a.getContent() != null ? a.getContent().getId() : null)
                .episodeId(a.getEpisode() != null ? a.getEpisode().getId() : null)
                .durationSeconds(a.getDurationSeconds())
                .drmEnabled(a.getDrmEnabled())
                .build();
    }
}
