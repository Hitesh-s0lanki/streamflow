package com.streamflow.service;

import com.streamflow.config.CommonConfig;
import com.streamflow.dto.AdminAnalyticsOverviewResponse;
import com.streamflow.dto.AnalyticsOverviewResponse;
import com.streamflow.dto.VideoAnalyticsRecordResponse;
import com.streamflow.entity.PlaybackAnalytics;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.PlaybackAnalyticsRepository;
import com.streamflow.repository.PlaybackWindowViewerRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read-only analytics for dashboards and demos (Phase 7).
 */
@Service
public class AnalyticsService {

    private final CommonConfig commonConfig;
    private final PlaybackAnalyticsRepository analyticsRepository;
    private final PlaybackWindowViewerRepository windowViewerRepository;
    private final VideoAssetRepository videoAssetRepository;
    private final IngestionJobRepository ingestionJobRepository;

    public AnalyticsService(CommonConfig commonConfig,
                            PlaybackAnalyticsRepository analyticsRepository,
                            PlaybackWindowViewerRepository windowViewerRepository,
                            VideoAssetRepository videoAssetRepository,
                            IngestionJobRepository ingestionJobRepository) {
        this.commonConfig = commonConfig;
        this.analyticsRepository = analyticsRepository;
        this.windowViewerRepository = windowViewerRepository;
        this.videoAssetRepository = videoAssetRepository;
        this.ingestionJobRepository = ingestionJobRepository;
    }

    /**
     * Get analytics records for a video, ordered by periodStart DESC. Optional from/to filter.
     */
    public List<VideoAnalyticsRecordResponse> getVideoAnalytics(UUID videoAssetId, Instant from, Instant to) {
        if (!videoAssetRepository.existsById(videoAssetId)) {
            throw new ResourceNotFoundException("VideoAsset", videoAssetId);
        }
        int maxPage = commonConfig.getMaxAnalyticsPage();
        List<PlaybackAnalytics> list;
        if (from != null && to != null) {
            list = analyticsRepository.findByVideoAssetIdAndPeriodStartBetween(
                    videoAssetId, from, to,
                    PageRequest.of(0, maxPage, Sort.by(Sort.Direction.DESC, "periodStart")));
        } else {
            list = analyticsRepository.findByVideoAssetIdOrderByPeriodStartDesc(
                    videoAssetId, PageRequest.of(0, maxPage, Sort.by(Sort.Direction.DESC, "periodStart")));
        }
        return list.stream()
                .map(this::toRecordResponse)
                .toList();
    }

    /**
     * Platform overview: totalVideos, totalPlays, totalUniqueViewers, avgCompletionRate, avgBufferingRate, topVideos.
     */
    public AnalyticsOverviewResponse getOverview(Instant from, Instant to) {
        long totalVideos = videoAssetRepository.count();
        long totalPlays = analyticsRepository.sumTotalPlays(from, to);
        long totalUniqueViewers = analyticsRepository.sumUniqueViewers(from, to);
        BigDecimal avgCompletionRate = analyticsRepository.avgCompletionRate(from, to);
        BigDecimal avgBufferingRate = analyticsRepository.avgBufferingRate(from, to);
        if (avgCompletionRate == null) avgCompletionRate = BigDecimal.ZERO;
        if (avgBufferingRate == null) avgBufferingRate = BigDecimal.ZERO;
        List<UUID> topIds = analyticsRepository.findTopVideoAssetIdsByTotalPlays(from, to,
                PageRequest.of(0, commonConfig.getDefaultTopVideos()));
        List<AnalyticsOverviewResponse.TopVideoSummary> topVideos = topIds.stream()
                .map(id -> new AnalyticsOverviewResponse.TopVideoSummary(id,
                        analyticsRepository.sumTotalPlaysByVideoAssetId(id, from, to)))
                .toList();
        return new AnalyticsOverviewResponse(totalVideos, totalPlays, totalUniqueViewers, avgCompletionRate, avgBufferingRate, topVideos);
    }

    /**
     * Admin overview: same as public overview plus ingestion readiness count (video assets with READY job).
     */
    public AdminAnalyticsOverviewResponse getAdminOverview(Instant from, Instant to) {
        AnalyticsOverviewResponse overview = getOverview(from, to);
        long ingestionReadyCount = ingestionJobRepository.countDistinctVideoAssetIdsByJobStatus(IngestionStatus.READY);
        return new AdminAnalyticsOverviewResponse(
                overview.totalVideos(),
                overview.totalPlays(),
                overview.totalUniqueViewers(),
                overview.avgCompletionRate(),
                overview.avgBufferingRate(),
                overview.topVideos(),
                ingestionReadyCount
        );
    }

    /**
     * Rebuild: delete analytics (and window viewers) in the given time range. Idempotent.
     * After delete, re-consumed or re-sent events will repopulate.
     */
    @org.springframework.transaction.annotation.Transactional
    public void rebuildAnalytics(Instant from, Instant to) {
        windowViewerRepository.deleteByPeriodStartBetween(from, to);
        analyticsRepository.deleteByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(from, to);
    }

    private VideoAnalyticsRecordResponse toRecordResponse(PlaybackAnalytics pa) {
        return new VideoAnalyticsRecordResponse(
                pa.getPeriodStart(),
                pa.getPeriodEnd(),
                pa.getTotalPlays(),
                pa.getUniqueViewers(),
                pa.getAvgWatchTimeSeconds(),
                pa.getCompletionRate(),
                pa.getBufferingRate()
        );
    }
}
