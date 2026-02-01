package com.streamflow.service;

import com.streamflow.entity.PlaybackAnalytics;
import com.streamflow.entity.VideoAsset;
import com.streamflow.repository.PlaybackAnalyticsRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlaybackAnalyticsService {

    private final PlaybackAnalyticsRepository playbackAnalyticsRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<PlaybackAnalytics> findById(UUID id) {
        return playbackAnalyticsRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<PlaybackAnalytics> findByVideoAssetId(UUID videoAssetId, Pageable pageable) {
        return playbackAnalyticsRepository.findByVideoAssetIdOrderByPeriodStartDesc(videoAssetId, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<PlaybackAnalytics> findByVideoAssetIdAndPeriodContaining(UUID videoAssetId, Instant time) {
        return playbackAnalyticsRepository.findByVideoAssetIdAndPeriodContaining(videoAssetId, time);
    }

    @Transactional(readOnly = true)
    public List<PlaybackAnalytics> findByVideoAssetIdAndPeriodBetween(UUID videoAssetId, Instant from, Instant to) {
        return playbackAnalyticsRepository.findByVideoAssetIdAndPeriodStartBetweenOrderByPeriodStartAsc(videoAssetId,
                from, to);
    }

    @Transactional
    public PlaybackAnalytics upsert(UUID videoAssetId, Instant periodStart, Instant periodEnd,
            Long totalPlays, Long uniqueViewers, Integer avgWatchTimeSeconds,
            BigDecimal completionRate, BigDecimal bufferingRate) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        PlaybackAnalytics analytics = playbackAnalyticsRepository
                .findByVideoAssetIdAndPeriodContaining(videoAssetId, periodStart)
                .orElse(null);
        if (analytics == null) {
            analytics = new PlaybackAnalytics();
            analytics.setVideoAsset(asset);
        }
        analytics.setPeriodStart(periodStart);
        analytics.setPeriodEnd(periodEnd);
        analytics.setTotalPlays(totalPlays);
        analytics.setUniqueViewers(uniqueViewers);
        analytics.setAvgWatchTimeSeconds(avgWatchTimeSeconds);
        analytics.setCompletionRate(completionRate);
        analytics.setBufferingRate(bufferingRate);
        return playbackAnalyticsRepository.save(analytics);
    }

    @Transactional
    public void deleteById(UUID id) {
        playbackAnalyticsRepository.deleteById(id);
    }
}
