package com.streamflow.service;

import com.streamflow.entity.SignedPlaybackUrl;
import com.streamflow.entity.VideoAsset;
import com.streamflow.repository.SignedPlaybackUrlRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignedPlaybackUrlService {

    private final SignedPlaybackUrlRepository signedPlaybackUrlRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<SignedPlaybackUrl> findById(UUID id) {
        return signedPlaybackUrlRepository.findById(id);
    }

    @Transactional
    public SignedPlaybackUrl create(UUID videoAssetId, String signedUrl, Instant expiresAt, String urlType) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        SignedPlaybackUrl record = new SignedPlaybackUrl();
        record.setVideoAsset(asset);
        record.setSignedUrl(signedUrl);
        record.setExpiresAt(expiresAt);
        record.setUrlType(urlType);
        return signedPlaybackUrlRepository.save(record);
    }

    @Transactional
    public int deleteExpiredBefore(Instant before) {
        return signedPlaybackUrlRepository.deleteExpiredBefore(before);
    }

    @Transactional
    public void deleteById(UUID id) {
        signedPlaybackUrlRepository.deleteById(id);
    }
}
