package com.streamflow.controller;

import com.streamflow.dto.*;
import com.streamflow.kafka.producer.KafkaProducerService;
import com.streamflow.service.PlaybackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Playback and DRM foundation: license request/validation, signed manifest and
 * segment URLs.
 * userId is derived from X-User-Id (Clerk integration); deviceId optional via
 * X-Device-Id or request body.
 */
@RestController
@RequestMapping("/api/playback")
public class PlaybackController {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_DEVICE_ID = "X-Device-Id";

    private final PlaybackService playbackService;
    private final KafkaProducerService kafkaProducerService;

    public PlaybackController(PlaybackService playbackService, KafkaProducerService kafkaProducerService) {
        this.playbackService = playbackService;
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Request playback license for a video. One ACTIVE license per user per
     * VideoAsset.
     * VideoAsset must exist and ingestion status must be READY.
     */
    @PostMapping("/license")
    public ResponseEntity<PlaybackLicenseResponse> requestLicense(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @RequestHeader(value = HEADER_DEVICE_ID, required = false) String deviceIdFromHeader,
            @Valid @RequestBody RequestPlaybackLicenseRequest request) {
        String deviceId = request.getDeviceId() != null ? request.getDeviceId() : deviceIdFromHeader;
        PlaybackLicenseResponse response = playbackService.requestLicense(
                userId, deviceId, request.getVideoAssetId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Validate license before serving playback URLs. License must be ACTIVE, not
     * expired, and match userId.
     */
    @GetMapping("/license/{licenseId}")
    public ResponseEntity<PlaybackLicenseResponse> validateLicense(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @PathVariable UUID licenseId) {
        PlaybackLicenseResponse response = playbackService.validateLicense(licenseId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Generate short-lived signed manifest URL. Valid license required; record
     * persisted for audit.
     */
    @PostMapping("/{videoAssetId}/manifest-url")
    public ResponseEntity<SignedPlaybackUrlResponse> generateManifestUrl(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @PathVariable UUID videoAssetId,
            @Valid @RequestBody ManifestUrlRequest request) {
        SignedPlaybackUrlResponse response = playbackService.generateManifestUrl(
                userId, videoAssetId, request.getLicenseId());
        return ResponseEntity.ok(response);
    }

    /**
     * Generate short-lived signed segment URL. Valid license required; segment path
     * must belong to a VideoVariant.
     */
    @PostMapping("/{videoAssetId}/segment-url")
    public ResponseEntity<SignedPlaybackUrlResponse> generateSegmentUrl(
            @RequestHeader(value = HEADER_USER_ID, required = false) String userId,
            @PathVariable UUID videoAssetId,
            @Valid @RequestBody SegmentUrlRequest request) {
        SignedPlaybackUrlResponse response = playbackService.generateSegmentUrl(
                userId, videoAssetId, request.getLicenseId(), request.getSegmentPath());
        return ResponseEntity.ok(response);
    }

    /**
     * Emit a playback event to Kafka for analytics (Phase 6/7). Consumer aggregates
     * into PlaybackAnalytics.
     */
    @PostMapping("/events")
    public ResponseEntity<Void> emitPlaybackEvent(@Valid @RequestBody PlaybackEventRequest request) {
        String userId = request.userId() != null ? request.userId() : null;
        kafkaProducerService.sendPlaybackEvent(
                request.eventType(),
                request.videoAssetId(),
                userId,
                request.currentTimeSeconds(),
                Instant.now());
        return ResponseEntity.accepted().build();
    }
}
