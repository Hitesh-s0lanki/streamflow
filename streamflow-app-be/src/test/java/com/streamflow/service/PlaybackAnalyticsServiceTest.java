package com.streamflow.service;

import com.streamflow.entity.PlaybackAnalytics;
import com.streamflow.entity.PlaybackWindowViewer;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.PlaybackEventType;
import com.streamflow.repository.PlaybackAnalyticsRepository;
import com.streamflow.repository.PlaybackWindowViewerRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.streamflow.config.TestFixtures.USER_ID;
import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackAnalyticsServiceTest {

    @Mock
    private PlaybackAnalyticsRepository analyticsRepository;
    @Mock
    private PlaybackWindowViewerRepository windowViewerRepository;
    @Mock
    private VideoAssetRepository videoAssetRepository;

    @InjectMocks
    private PlaybackAnalyticsService playbackAnalyticsService;

    @Nested
    @DisplayName("recordEvent")
    class RecordEvent {

        @Test
        @DisplayName("skips when videoAssetId is null")
        void skipsWhenVideoAssetIdNull() {
            playbackAnalyticsService.recordEvent(null, PlaybackEventType.PLAY, USER_ID, null, Instant.now());

            verify(analyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("skips when video asset not found")
        void skipsWhenVideoAssetNotFound() {
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.empty());

            playbackAnalyticsService.recordEvent(VIDEO_ASSET_ID, PlaybackEventType.PLAY, USER_ID, null, Instant.now());

            verify(analyticsRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates analytics and increments totalPlays for PLAY event")
        void createsAnalyticsAndIncrementsPlays() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(analyticsRepository.findByVideoAssetIdAndPeriodStart(eq(VIDEO_ASSET_ID), any()))
                    .thenReturn(Optional.empty());
            PlaybackAnalytics saved = new PlaybackAnalytics();
            saved.setVideoAsset(asset);
            saved.setTotalPlays(0L);
            when(analyticsRepository.save(any(PlaybackAnalytics.class))).thenAnswer(i -> {
                PlaybackAnalytics a = i.getArgument(0);
                a.setTotalPlays(a.getTotalPlays() != null ? a.getTotalPlays() : 0L);
                return a;
            });
            when(windowViewerRepository.findByVideoAssetIdAndPeriodStartAndUserId(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(windowViewerRepository.countByVideoAssetIdAndPeriodStart(any(), any())).thenReturn(1L);

            Instant ts = Instant.parse("2024-01-15T14:00:00Z");
            playbackAnalyticsService.recordEvent(VIDEO_ASSET_ID, PlaybackEventType.PLAY, USER_ID, null, ts);

            ArgumentCaptor<PlaybackAnalytics> captor = ArgumentCaptor.forClass(PlaybackAnalytics.class);
            verify(analyticsRepository, times(2)).save(captor.capture());
            assertThat(captor.getValue().getTotalPlays()).isEqualTo(1L);
        }

        @Test
        @DisplayName("recordEventFromJson parses valid JSON and calls recordEvent")
        void recordEventFromJsonParsesAndRecords() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(analyticsRepository.findByVideoAssetIdAndPeriodStart(eq(VIDEO_ASSET_ID), any()))
                    .thenReturn(Optional.empty());
            when(analyticsRepository.save(any(PlaybackAnalytics.class))).thenAnswer(i -> i.getArgument(0));
            when(windowViewerRepository.findByVideoAssetIdAndPeriodStartAndUserId(any(), any(), any()))
                    .thenReturn(Optional.empty());
            when(windowViewerRepository.countByVideoAssetIdAndPeriodStart(any(), any())).thenReturn(1L);

            String json = "{\"eventType\":\"PLAY\",\"videoAssetId\":\"" + VIDEO_ASSET_ID + "\",\"userId\":\"u1\",\"timestamp\":\"2024-01-15T14:00:00Z\"}";
            playbackAnalyticsService.recordEventFromJson(json);

            verify(analyticsRepository, times(2)).save(any(PlaybackAnalytics.class));
        }
    }
}
