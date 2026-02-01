package com.streamflow.service;

import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.WatchProgress;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.WatchProgressRepository;
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
public class WatchProgressService {

    private final WatchProgressRepository watchProgressRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<WatchProgress> findByUserIdAndVideoAssetId(String userId, UUID videoAssetId) {
        return watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId);
    }

    @Transactional(readOnly = true)
    public List<WatchProgress> findContinueWatching(String userId, Pageable pageable) {
        return watchProgressRepository.findContinueWatching(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<WatchProgress> findByUserId(String userId, Pageable pageable) {
        return watchProgressRepository.findByUserIdOrderByLastWatchedAtDesc(userId, pageable);
    }

    @Transactional
    public WatchProgress upsert(String userId, UUID videoAssetId, int lastWatchedSecond, boolean completed) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId)
                .orElseGet(() -> {
                    WatchProgress p = new WatchProgress();
                    p.setUserId(userId);
                    p.setVideoAsset(asset);
                    return p;
                });
        progress.setLastWatchedSecond(lastWatchedSecond);
        progress.setCompleted(completed);
        progress.setLastWatchedAt(Instant.now());
        return watchProgressRepository.save(progress);
    }

    @Transactional
    public void deleteByUserIdAndVideoAssetId(String userId, UUID videoAssetId) {
        watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId)
                .ifPresent(watchProgressRepository::delete);
    }

    @Transactional
    public void deleteById(UUID id) {
        watchProgressRepository.deleteById(id);
    }
}
