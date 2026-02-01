package com.streamflow.service;

import com.streamflow.dto.PagedResponse;
import com.streamflow.dto.PlaybackLicenseResponse;
import com.streamflow.dto.SignedPlaybackUrlResponse;
import com.streamflow.dto.PlaybackEventLogItemResponse;
import com.streamflow.dto.SignedUrlAuditItemResponse;
import com.streamflow.entity.PlaybackEventLog;
import com.streamflow.entity.PlaybackLicense;
import com.streamflow.entity.SignedPlaybackUrl;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.VideoVariant;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.entity.enums.LicenseStatus;
import com.streamflow.entity.enums.PlaybackEventType;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.PlaybackEventLogRepository;
import com.streamflow.repository.PlaybackLicenseRepository;
import com.streamflow.repository.SignedPlaybackUrlRepository;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.VideoVariantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Playback and DRM foundation: licenses, signed manifest/segment URLs, revocation.
 * Playback access requires a valid license; VideoAsset must be READY.
 */
@Service
public class PlaybackService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackService.class);
    private static final String URL_TYPE_MANIFEST = "MANIFEST";
    private static final String URL_TYPE_SEGMENT = "SEGMENT";

    private final PlaybackLicenseRepository playbackLicenseRepository;
    private final SignedPlaybackUrlRepository signedPlaybackUrlRepository;
    private final PlaybackEventLogRepository playbackEventLogRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final VideoVariantRepository videoVariantRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final Optional<S3StorageService> s3StorageService;

    @Value("${streamflow.playback.license-ttl-minutes:60}")
    private int licenseTtlMinutes = 60;

    @Value("${streamflow.playback.signed-url-expiration-minutes:10}")
    private int signedUrlExpirationMinutes = 10;

    public PlaybackService(
            PlaybackLicenseRepository playbackLicenseRepository,
            SignedPlaybackUrlRepository signedPlaybackUrlRepository,
            PlaybackEventLogRepository playbackEventLogRepository,
            VideoAssetRepository videoAssetRepository,
            VideoVariantRepository videoVariantRepository,
            IngestionJobRepository ingestionJobRepository,
            @Autowired(required = false) S3StorageService s3StorageService) {
        this.playbackLicenseRepository = playbackLicenseRepository;
        this.signedPlaybackUrlRepository = signedPlaybackUrlRepository;
        this.playbackEventLogRepository = playbackEventLogRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.videoVariantRepository = videoVariantRepository;
        this.ingestionJobRepository = ingestionJobRepository;
        this.s3StorageService = Optional.ofNullable(s3StorageService);
    }

    /**
     * Request playback license for a video. One ACTIVE license per user per VideoAsset;
     * returns existing active license or creates a new one. VideoAsset must exist and be READY.
     */
    @Transactional
    public PlaybackLicenseResponse requestLicense(String userId, String deviceId, UUID videoAssetId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required (e.g. from X-User-Id header)");
        }
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        if (!ingestionJobRepository.existsByVideoAssetIdAndJobStatus(videoAssetId, IngestionStatus.READY)) {
            throw new BadRequestException("VideoAsset is not ready for playback; ingestion status must be READY");
        }
        Instant now = Instant.now();
        Optional<PlaybackLicense> existing = playbackLicenseRepository.findActiveLicense(
                userId, videoAssetId, LicenseStatus.ACTIVE, now);
        if (existing.isPresent()) {
            return toLicenseResponse(existing.get());
        }
        Instant expiresAt = now.plusSeconds(licenseTtlMinutes * 60L);
        PlaybackLicense license = new PlaybackLicense();
        license.setUserId(userId);
        license.setVideoAsset(asset);
        license.setExpiresAt(expiresAt);
        license.setLicenseStatus(LicenseStatus.ACTIVE);
        if (deviceId != null && !deviceId.isBlank()) {
            license.setDeviceId(deviceId);
        }
        license = playbackLicenseRepository.save(license);
        log.info("Created playback license: licenseId={}, userId={}, videoAssetId={}, expiresAt={}",
                license.getId(), userId, videoAssetId, expiresAt);
        return toLicenseResponse(license);
    }

    /**
     * Validate license before serving playback URLs. License must be ACTIVE, not expired, and match userId.
     */
    @Transactional(readOnly = true)
    public PlaybackLicenseResponse validateLicense(UUID licenseId, String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required (e.g. from X-User-Id header)");
        }
        PlaybackLicense license = playbackLicenseRepository.findByIdAndUserId(licenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaybackLicense", licenseId));
        if (license.getLicenseStatus() != LicenseStatus.ACTIVE) {
            throw new BadRequestException("License is not active; status=" + license.getLicenseStatus());
        }
        if (license.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("License has expired");
        }
        return toLicenseResponse(license);
    }

    /**
     * Generate short-lived signed manifest URL. Valid license required; SignedPlaybackUrl record persisted for audit.
     */
    @Transactional
    public SignedPlaybackUrlResponse generateManifestUrl(String userId, UUID videoAssetId, UUID licenseId) {
        PlaybackLicense license = requireValidLicenseForAsset(userId, videoAssetId, licenseId);
        VideoAsset asset = license.getVideoAsset();
        String manifestUrl = asset.getManifestUrl();
        if (manifestUrl == null || manifestUrl.isBlank()) {
            throw new BadRequestException("VideoAsset has no manifest URL configured");
        }
        if (manifestUrl.startsWith("http") && !manifestUrl.contains("s3.amazonaws.com") && !manifestUrl.contains(".s3.")) {
            throw new BadRequestException("Manifest URL must be an S3 key or S3 object URL for signed playback");
        }
        S3StorageService s3 = s3StorageService.orElseThrow(() ->
                new BadRequestException("S3 is not configured; signed playback URLs are not available"));
        String manifestKey = manifestUrl.startsWith("http") ? extractS3KeyFromUrl(manifestUrl) : manifestUrl;
        if (manifestKey.startsWith("http")) {
            throw new BadRequestException("Manifest URL must be an S3 key or S3 object URL for signed playback");
        }
        var result = s3.generatePresignedGetUrl(manifestKey, signedUrlExpirationMinutes);
        SignedPlaybackUrl record = new SignedPlaybackUrl();
        record.setVideoAsset(asset);
        record.setPlaybackLicense(license);
        record.setSignedUrl(result.getUrl());
        record.setExpiresAt(result.getExpiresAt());
        record.setUrlType(URL_TYPE_MANIFEST);
        signedPlaybackUrlRepository.save(record);
        log.info("Generated signed manifest URL: videoAssetId={}, licenseId={}, expiresAt={}",
                videoAssetId, licenseId, result.getExpiresAt());
        return SignedPlaybackUrlResponse.builder()
                .signedUrl(result.getUrl())
                .expiresAt(result.getExpiresAt())
                .build();
    }

    /**
     * Generate short-lived signed segment URL. Valid license required; segment path must belong to a VideoVariant.
     */
    @Transactional
    public SignedPlaybackUrlResponse generateSegmentUrl(String userId, UUID videoAssetId, UUID licenseId, String segmentPath) {
        PlaybackLicense license = requireValidLicenseForAsset(userId, videoAssetId, licenseId);
        VideoAsset asset = license.getVideoAsset();
        List<VideoVariant> variants = videoVariantRepository.findByVideoAssetIdOrderBySortOrderAsc(videoAssetId);
        String normalizedSegment = segmentPath.startsWith("/") ? segmentPath.substring(1) : segmentPath;
        boolean belongsToVariant = variants.stream()
                .map(VideoVariant::getSegmentPath)
                .filter(p -> p != null && !p.isBlank())
                .anyMatch(p -> {
                    String base = p.endsWith("/") ? p : (p + "/");
                    return normalizedSegment.equals(p) || normalizedSegment.startsWith(base);
                });
        if (!belongsToVariant) {
            throw new BadRequestException("Segment path does not belong to any VideoVariant of this asset");
        }
        S3StorageService s3 = s3StorageService.orElseThrow(() ->
                new BadRequestException("S3 is not configured; signed playback URLs are not available"));
        var result = s3.generatePresignedGetUrl(normalizedSegment, signedUrlExpirationMinutes);
        SignedPlaybackUrl record = new SignedPlaybackUrl();
        record.setVideoAsset(asset);
        record.setPlaybackLicense(license);
        record.setSignedUrl(result.getUrl());
        record.setExpiresAt(result.getExpiresAt());
        record.setUrlType(URL_TYPE_SEGMENT);
        signedPlaybackUrlRepository.save(record);
        log.info("Generated signed segment URL: videoAssetId={}, licenseId={}, expiresAt={}",
                videoAssetId, licenseId, result.getExpiresAt());
        return SignedPlaybackUrlResponse.builder()
                .signedUrl(result.getUrl())
                .expiresAt(result.getExpiresAt())
                .build();
    }

    /**
     * Revoke a playback license (admin). All future signed URL requests for this license will fail.
     */
    @Transactional
    public PlaybackLicenseResponse revokeLicense(UUID licenseId) {
        PlaybackLicense license = playbackLicenseRepository.findById(licenseId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaybackLicense", licenseId));
        license.setLicenseStatus(LicenseStatus.REVOKED);
        playbackLicenseRepository.save(license);
        log.info("Revoked playback license: licenseId={}", licenseId);
        return toLicenseResponse(license);
    }

    /** Admin: list licenses with filters. Order: createdAt DESC. Paginated. */
    @Transactional(readOnly = true)
    public PagedResponse<PlaybackLicenseResponse> adminListLicenses(String userId, UUID videoAssetId,
            LicenseStatus status, Instant expiresFrom, Instant expiresTo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PlaybackLicense> p = playbackLicenseRepository.findAdminList(
                userId, videoAssetId, status, expiresFrom, expiresTo, pageable);
        List<PlaybackLicenseResponse> items = p.getContent().stream()
                .map(PlaybackService::toLicenseResponse)
                .toList();
        return PagedResponse.<PlaybackLicenseResponse>builder()
                .content(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    /** Admin: signed URL audit trail (metadata only, no active signed URLs in response). */
    @Transactional(readOnly = true)
    public PagedResponse<SignedUrlAuditItemResponse> adminListSignedUrls(UUID videoAssetId, String urlType,
            Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SignedPlaybackUrl> p = signedPlaybackUrlRepository.findAdminList(
                videoAssetId, urlType, from, to, pageable);
        List<SignedUrlAuditItemResponse> items = p.getContent().stream()
                .map(this::toSignedUrlAuditItem)
                .toList();
        return PagedResponse.<SignedUrlAuditItemResponse>builder()
                .content(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    private SignedUrlAuditItemResponse toSignedUrlAuditItem(SignedPlaybackUrl s) {
        return SignedUrlAuditItemResponse.builder()
                .id(s.getId())
                .videoAssetId(s.getVideoAsset().getId())
                .licenseId(s.getPlaybackLicense() != null ? s.getPlaybackLicense().getId() : null)
                .urlType(s.getUrlType())
                .expiresAt(s.getExpiresAt())
                .createdAt(s.getCreatedAt())
                .build();
    }

    /** Admin: list sampled playback events. Order: newest first. Paginated (limit = size). */
    @Transactional(readOnly = true)
    public PagedResponse<PlaybackEventLogItemResponse> adminListPlaybackEvents(String userId, UUID videoAssetId,
            PlaybackEventType eventType, Instant from, Instant to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PlaybackEventLog> p = playbackEventLogRepository.findAdminList(
                userId, videoAssetId, eventType, from, to, pageable);
        List<PlaybackEventLogItemResponse> items = p.getContent().stream()
                .map(this::toPlaybackEventLogItem)
                .toList();
        return PagedResponse.<PlaybackEventLogItemResponse>builder()
                .content(items)
                .page(p.getNumber())
                .size(p.getSize())
                .totalElements(p.getTotalElements())
                .totalPages(p.getTotalPages())
                .first(p.isFirst())
                .last(p.isLast())
                .build();
    }

    private PlaybackEventLogItemResponse toPlaybackEventLogItem(PlaybackEventLog e) {
        return PlaybackEventLogItemResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .videoAssetId(e.getVideoAsset() != null ? e.getVideoAsset().getId() : null)
                .eventType(e.getEventType())
                .currentTimeSeconds(e.getCurrentTimeSeconds())
                .payload(e.getPayload())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private PlaybackLicense requireValidLicenseForAsset(String userId, UUID videoAssetId, UUID licenseId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required (e.g. from X-User-Id header)");
        }
        PlaybackLicense license = playbackLicenseRepository.findByIdAndUserId(licenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PlaybackLicense", licenseId));
        if (license.getLicenseStatus() != LicenseStatus.ACTIVE) {
            throw new BadRequestException("License is not active; status=" + license.getLicenseStatus());
        }
        if (license.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("License has expired");
        }
        if (!license.getVideoAsset().getId().equals(videoAssetId)) {
            throw new BadRequestException("License does not apply to this video asset");
        }
        return license;
    }

    private static PlaybackLicenseResponse toLicenseResponse(PlaybackLicense license) {
        return PlaybackLicenseResponse.builder()
                .licenseId(license.getId())
                .videoAssetId(license.getVideoAsset().getId())
                .userId(license.getUserId())
                .deviceId(license.getDeviceId())
                .expiresAt(license.getExpiresAt())
                .licenseStatus(license.getLicenseStatus())
                .build();
    }

    /** If manifest URL is a full S3 URL, extract key; otherwise return as-is. */
    private static String extractS3KeyFromUrl(String url) {
        if (url == null || !url.startsWith("http")) return url;
        int idx = url.indexOf(".s3.");
        if (idx == -1) idx = url.indexOf("s3.amazonaws.com/");
        if (idx != -1) {
            int pathStart = url.indexOf("/", idx) + 1;
            int bucketEnd = url.indexOf("/", pathStart);
            if (bucketEnd != -1) return url.substring(bucketEnd + 1);
        }
        return url;
    }
}
