package com.streamflow.service;

import com.streamflow.config.CommonConfig;
import com.streamflow.dto.ContinueWatchingItemResponse;
import com.streamflow.dto.WatchProgressResponse;
import com.streamflow.entity.Episode;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.WatchProgress;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.WatchProgressRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Watch progress and resume: upsert progress, get position, mark completed, continue watching row.
 * Progress is keyed by (userId, videoAssetId). userId comes from Clerk (X-User-Id).
 */
@Service
public class WatchProgressService {

    private final CommonConfig commonConfig;
    private final WatchProgressRepository watchProgressRepository;
    private final VideoAssetRepository videoAssetRepository;

    public WatchProgressService(CommonConfig commonConfig,
                               WatchProgressRepository watchProgressRepository,
                               VideoAssetRepository videoAssetRepository) {
        this.commonConfig = commonConfig;
        this.watchProgressRepository = watchProgressRepository;
        this.videoAssetRepository = videoAssetRepository;
    }

    /**
     * Upsert watch progress. Idempotent and safe to call frequently.
     * Clamps lastWatchedSecond to [0, durationSeconds]. Auto-sets completed when near end.
     */
    @Transactional
    public WatchProgressResponse upsert(String userId, UUID videoAssetId, int lastWatchedSecond, Boolean completedFromClient) {
        requireUserId(userId);
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        int durationSeconds = asset.getDurationSeconds();
        int clamped = Math.max(0, Math.min(lastWatchedSecond, durationSeconds));

        int threshold = commonConfig.getCompletionThresholdSeconds();
        boolean completed;
        if (completedFromClient != null) {
            completed = completedFromClient;
        } else {
            completed = clamped >= Math.max(0, durationSeconds - threshold);
        }

        Instant now = Instant.now();
        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId)
                .orElseGet(() -> {
                    WatchProgress p = new WatchProgress();
                    p.setUserId(userId);
                    p.setVideoAsset(asset);
                    p.setLastWatchedSecond(0);
                    p.setCompleted(false);
                    watchProgressRepository.save(p);
                    return p;
                });

        progress.setLastWatchedSecond(clamped);
        progress.setCompleted(completed);
        progress.setLastWatchedAt(now);
        watchProgressRepository.save(progress);

        return toResponse(progress);
    }

    /**
     * Get progress for a video asset. Returns default (lastWatchedSecond=0, completed=false) if no record.
     */
    public WatchProgressResponse getByVideoAsset(String userId, UUID videoAssetId) {
        requireUserId(userId);
        return watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId)
                .map(this::toResponse)
                .orElse(WatchProgressResponse.builder()
                        .videoAssetId(videoAssetId)
                        .lastWatchedSecond(0)
                        .completed(false)
                        .lastWatchedAt(null)
                        .build());
    }

    /**
     * Mark video as completed. Upserts if missing; sets completed=true and lastWatchedSecond to duration.
     */
    @Transactional
    public WatchProgressResponse markComplete(String userId, UUID videoAssetId) {
        requireUserId(userId);
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new ResourceNotFoundException("VideoAsset", videoAssetId));
        int durationSeconds = asset.getDurationSeconds();
        Instant now = Instant.now();

        WatchProgress progress = watchProgressRepository.findByUserIdAndVideoAssetId(userId, videoAssetId)
                .orElseGet(() -> {
                    WatchProgress p = new WatchProgress();
                    p.setUserId(userId);
                    p.setVideoAsset(asset);
                    p.setLastWatchedSecond(0);
                    p.setCompleted(false);
                    watchProgressRepository.save(p);
                    return p;
                });

        progress.setLastWatchedSecond(durationSeconds);
        progress.setCompleted(true);
        progress.setLastWatchedAt(now);
        watchProgressRepository.save(progress);

        return toResponse(progress);
    }

    /**
     * Continue watching row: in-progress only, last 30 days, ordered by lastWatchedAt DESC.
     */
    public List<ContinueWatchingItemResponse> getContinueWatching(String userId) {
        requireUserId(userId);
        Instant since = Instant.now().minusSeconds((long) commonConfig.getContinueWatchingDays() * 24 * 60 * 60);
        List<WatchProgress> list = watchProgressRepository.findContinueWatchingSince(
                userId, since, PageRequest.of(0, commonConfig.getContinueWatchingLimit()));
        return list.stream()
                .map(this::toContinueWatchingItem)
                .collect(Collectors.toList());
    }

    private void requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("userId is required (e.g. from X-User-Id header)");
        }
    }

    private WatchProgressResponse toResponse(WatchProgress wp) {
        return WatchProgressResponse.builder()
                .videoAssetId(wp.getVideoAsset().getId())
                .lastWatchedSecond(wp.getLastWatchedSecond())
                .completed(wp.getCompleted())
                .lastWatchedAt(wp.getLastWatchedAt())
                .build();
    }

    private ContinueWatchingItemResponse toContinueWatchingItem(WatchProgress wp) {
        VideoAsset va = wp.getVideoAsset();
        String title = null;
        String episodeTitle = null;
        String posterUrl = null;
        String thumbnailUrl = null;
        UUID contentId = null;
        UUID episodeId = null;

        if (va.getContent() != null) {
            title = va.getContent().getTitle();
            posterUrl = va.getContent().getPosterUrl();
            thumbnailUrl = va.getContent().getThumbnailUrl();
            contentId = va.getContent().getId();
        }
        Episode ep = va.getEpisode();
        if (ep != null) {
            episodeId = ep.getId();
            episodeTitle = ep.getTitle();
            if (ep.getThumbnailUrl() != null) {
                thumbnailUrl = ep.getThumbnailUrl();
            }
        }

        return ContinueWatchingItemResponse.builder()
                .videoAssetId(va.getId())
                .contentId(contentId)
                .episodeId(episodeId)
                .title(title)
                .episodeTitle(episodeTitle)
                .posterUrl(posterUrl)
                .thumbnailUrl(thumbnailUrl)
                .lastWatchedSecond(wp.getLastWatchedSecond())
                .durationSeconds(va.getDurationSeconds())
                .lastWatchedAt(wp.getLastWatchedAt())
                .build();
    }
}
