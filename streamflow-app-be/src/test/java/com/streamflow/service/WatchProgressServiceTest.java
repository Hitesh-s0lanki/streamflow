package com.streamflow.service;

import com.streamflow.config.CommonConfig;
import com.streamflow.dto.WatchProgressResponse;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.WatchProgress;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.WatchProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.streamflow.config.TestFixtures.USER_ID;
import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchProgressServiceTest {

    @Mock
    private CommonConfig commonConfig;
    @Mock
    private WatchProgressRepository watchProgressRepository;
    @Mock
    private VideoAssetRepository videoAssetRepository;

    @InjectMocks
    private WatchProgressService watchProgressService;

    @BeforeEach
    void setUp() {
        lenient().when(commonConfig.getCompletionThresholdSeconds()).thenReturn(60);
        lenient().when(commonConfig.getContinueWatchingDays()).thenReturn(30);
        lenient().when(commonConfig.getContinueWatchingLimit()).thenReturn(50);
    }

    @Nested
    @DisplayName("upsert")
    class Upsert {

        @Test
        @DisplayName("throws when userId is blank")
        void throwsWhenUserIdBlank() {
            assertThatThrownBy(() -> watchProgressService.upsert(" ", VIDEO_ASSET_ID, 0, null))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> watchProgressService.upsert(USER_ID, VIDEO_ASSET_ID, 0, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VideoAsset");
        }

        @Test
        @DisplayName("creates progress and returns response")
        void createsProgressAndReturnsResponse() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            asset.setDurationSeconds(300);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(watchProgressRepository.findByUserIdAndVideoAssetId(USER_ID, VIDEO_ASSET_ID))
                    .thenReturn(Optional.empty());
            when(watchProgressRepository.save(any(WatchProgress.class))).thenAnswer(i -> i.getArgument(0));

            WatchProgressResponse response = watchProgressService.upsert(USER_ID, VIDEO_ASSET_ID, 100, false);

            assertThat(response.getVideoAssetId()).isEqualTo(VIDEO_ASSET_ID);
            assertThat(response.getLastWatchedSecond()).isEqualTo(100);
            assertThat(response.getCompleted()).isFalse();
            verify(watchProgressRepository, times(2)).save(any(WatchProgress.class));
        }
    }

    @Nested
    @DisplayName("getByVideoAsset")
    class GetByVideoAsset {

        @Test
        @DisplayName("throws when userId is blank")
        void throwsWhenUserIdBlank() {
            assertThatThrownBy(() -> watchProgressService.getByVideoAsset(" ", VIDEO_ASSET_ID))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("returns default when no progress")
        void returnsDefaultWhenNoProgress() {
            when(watchProgressRepository.findByUserIdAndVideoAssetId(USER_ID, VIDEO_ASSET_ID))
                    .thenReturn(Optional.empty());

            WatchProgressResponse response = watchProgressService.getByVideoAsset(USER_ID, VIDEO_ASSET_ID);

            assertThat(response.getVideoAssetId()).isEqualTo(VIDEO_ASSET_ID);
            assertThat(response.getLastWatchedSecond()).isEqualTo(0);
            assertThat(response.getCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("markComplete")
    class MarkComplete {

        @Test
        @DisplayName("marks progress completed")
        void marksComplete() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            asset.setDurationSeconds(300);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(watchProgressRepository.findByUserIdAndVideoAssetId(USER_ID, VIDEO_ASSET_ID))
                    .thenReturn(Optional.empty());
            when(watchProgressRepository.save(any(WatchProgress.class))).thenAnswer(i -> i.getArgument(0));

            WatchProgressResponse response = watchProgressService.markComplete(USER_ID, VIDEO_ASSET_ID);

            assertThat(response.getCompleted()).isTrue();
            assertThat(response.getLastWatchedSecond()).isEqualTo(300);
        }
    }
}
