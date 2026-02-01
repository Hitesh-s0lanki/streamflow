package com.streamflow.service;

import com.streamflow.entity.PlaybackLicense;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.LicenseStatus;
import com.streamflow.repository.PlaybackLicenseRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackLicenseService {

    private final PlaybackLicenseRepository playbackLicenseRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<PlaybackLicense> findById(UUID id) {
        return playbackLicenseRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<PlaybackLicense> findActiveLicense(String userId, UUID videoAssetId) {
        return playbackLicenseRepository.findActiveLicense(userId, videoAssetId, LicenseStatus.ACTIVE, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<PlaybackLicense> findByUserId(String userId, Pageable pageable) {
        return playbackLicenseRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<PlaybackLicense> findByVideoAssetIdAndUserId(UUID videoAssetId, String userId) {
        return playbackLicenseRepository.findByVideoAssetIdAndUserIdAndLicenseStatus(videoAssetId, userId,
                LicenseStatus.ACTIVE);
    }

    @Transactional
    public PlaybackLicense issue(String userId, UUID videoAssetId, Instant expiresAt, String deviceId) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        if (playbackLicenseRepository.existsByUserIdAndVideoAssetIdAndLicenseStatusAndExpiresAtAfter(userId,
                videoAssetId,
                LicenseStatus.ACTIVE, Instant.now())) {
            throw new IllegalStateException("Active license already exists for user and video asset");
        }
        PlaybackLicense license = new PlaybackLicense();
        license.setUserId(userId);
        license.setVideoAsset(asset);
        license.setExpiresAt(expiresAt);
        license.setLicenseStatus(LicenseStatus.ACTIVE);
        license.setDeviceId(deviceId);
        return playbackLicenseRepository.save(license);
    }

    @Transactional
    public PlaybackLicense revoke(UUID id) {
        PlaybackLicense license = playbackLicenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PlaybackLicense not found: " + id));
        license.setLicenseStatus(LicenseStatus.REVOKED);
        return playbackLicenseRepository.save(license);
    }

    @Transactional
    public void deleteById(UUID id) {
        playbackLicenseRepository.deleteById(id);
    }
}
