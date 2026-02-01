package com.streamflow.service;

import com.streamflow.config.CommonConfig;
import com.streamflow.dto.AnalyticsOverviewResponse;
import com.streamflow.dto.VideoAnalyticsRecordResponse;
import com.streamflow.entity.PlaybackAnalytics;
import com.streamflow.entity.VideoAsset;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.PlaybackAnalyticsRepository;
import com.streamflow.repository.PlaybackWindowViewerRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CommonConfig commonConfig;
    @Mock
    private PlaybackAnalyticsRepository analyticsRepository;
    @Mock
    private PlaybackWindowViewerRepository windowViewerRepository;
    @Mock
    private VideoAssetRepository videoAssetRepository;
    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        lenient().when(commonConfig.getMaxAnalyticsPage()).thenReturn(500);
        lenient().when(commonConfig.getDefaultTopVideos()).thenReturn(10);
    }

    @Nested
    @DisplayName("getVideoAnalytics")
    class GetVideoAnalytics {

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.existsById(VIDEO_ASSET_ID)).thenReturn(false);

            assertThatThrownBy(() -> analyticsService.getVideoAnalytics(VIDEO_ASSET_ID, null, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VideoAsset");
        }

        @Test
        @DisplayName("returns list when asset exists and no date range")
        void returnsListWhenAssetExistsNoRange() {
            when(videoAssetRepository.existsById(VIDEO_ASSET_ID)).thenReturn(true);
            PlaybackAnalytics pa = new PlaybackAnalytics();
            pa.setPeriodStart(Instant.EPOCH);
            pa.setPeriodEnd(Instant.EPOCH.plusSeconds(3600));
            pa.setTotalPlays(5L);
            pa.setUniqueViewers(3L);
            pa.setAvgWatchTimeSeconds(120);
            pa.setCompletionRate(BigDecimal.valueOf(0.8));
            pa.setBufferingRate(BigDecimal.ZERO);
            when(analyticsRepository.findByVideoAssetIdOrderByPeriodStartDesc(
                    eq(VIDEO_ASSET_ID), any(PageRequest.class))).thenReturn(List.of(pa));

            List<VideoAnalyticsRecordResponse> result =
                    analyticsService.getVideoAnalytics(VIDEO_ASSET_ID, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).totalPlays()).isEqualTo(5L);
            assertThat(result.get(0).uniqueViewers()).isEqualTo(3L);
            verify(videoAssetRepository).existsById(VIDEO_ASSET_ID);
        }

        @Test
        @DisplayName("uses date range when from and to provided")
        void usesDateRangeWhenFromAndToProvided() {
            Instant from = Instant.EPOCH;
            Instant to = Instant.EPOCH.plusSeconds(86400);
            when(videoAssetRepository.existsById(VIDEO_ASSET_ID)).thenReturn(true);
            when(analyticsRepository.findByVideoAssetIdAndPeriodStartBetween(
                    eq(VIDEO_ASSET_ID), eq(from), eq(to), any(PageRequest.class))).thenReturn(List.of());

            analyticsService.getVideoAnalytics(VIDEO_ASSET_ID, from, to);

            verify(analyticsRepository).findByVideoAssetIdAndPeriodStartBetween(
                    eq(VIDEO_ASSET_ID), eq(from), eq(to),
                    eq(PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "periodStart"))));
        }
    }

    @Nested
    @DisplayName("getOverview")
    class GetOverview {

        @Test
        @DisplayName("returns overview with totals and top videos")
        void returnsOverview() {
            when(videoAssetRepository.count()).thenReturn(10L);
            when(analyticsRepository.sumTotalPlays(any(), any())).thenReturn(100L);
            when(analyticsRepository.sumUniqueViewers(any(), any())).thenReturn(50L);
            when(analyticsRepository.avgCompletionRate(any(), any())).thenReturn(BigDecimal.valueOf(0.75));
            when(analyticsRepository.avgBufferingRate(any(), any())).thenReturn(BigDecimal.valueOf(0.02));
            when(analyticsRepository.findTopVideoAssetIdsByTotalPlays(any(), any(), any()))
                    .thenReturn(List.of(VIDEO_ASSET_ID));
            when(analyticsRepository.sumTotalPlaysByVideoAssetId(eq(VIDEO_ASSET_ID), any(), any())).thenReturn(20L);

            AnalyticsOverviewResponse result = analyticsService.getOverview(Instant.EPOCH, Instant.now());

            assertThat(result.totalVideos()).isEqualTo(10L);
            assertThat(result.totalPlays()).isEqualTo(100L);
            assertThat(result.totalUniqueViewers()).isEqualTo(50L);
            assertThat(result.avgCompletionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.75));
            assertThat(result.topVideos()).hasSize(1);
            assertThat(result.topVideos().get(0).videoAssetId()).isEqualTo(VIDEO_ASSET_ID);
            assertThat(result.topVideos().get(0).totalPlays()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("rebuildAnalytics")
    class RebuildAnalytics {

        @Test
        @DisplayName("deletes window viewers and analytics in range")
        void deletesInRange() {
            Instant from = Instant.EPOCH;
            Instant to = Instant.EPOCH.plusSeconds(3600);

            analyticsService.rebuildAnalytics(from, to);

            verify(windowViewerRepository).deleteByPeriodStartBetween(from, to);
            verify(analyticsRepository).deleteByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(from, to);
        }
    }
}
