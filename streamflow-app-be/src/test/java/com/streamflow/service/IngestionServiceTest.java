package com.streamflow.service;

import com.streamflow.dto.ConfirmUploadRequest;
import com.streamflow.dto.IngestionStatusResponse;
import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.VideoAsset;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.kafka.producer.KafkaProducerService;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static com.streamflow.config.TestFixtures.confirmUploadRequest;
import static com.streamflow.entity.enums.IngestionStatus.PENDING;
import static com.streamflow.entity.enums.IngestionStatus.UPLOADED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VideoAssetRepository videoAssetRepository;
    @Mock
    private IngestionJobRepository ingestionJobRepository;
    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private IngestionService ingestionService;

    @Nested
    @DisplayName("confirmUpload")
    class ConfirmUpload {

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.empty());
            ConfirmUploadRequest request = confirmUploadRequest("videos/raw/key", "video/mp4");

            assertThatThrownBy(() -> ingestionService.confirmUpload(VIDEO_ASSET_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VideoAsset");
        }

        @Test
        @DisplayName("creates job UPLOADED and sends Kafka event")
        void createsJobAndSendsKafka() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            IngestionJob saved = new IngestionJob();
            saved.setId(java.util.UUID.randomUUID());
            saved.setVideoAsset(asset);
            saved.setJobStatus(UPLOADED);
            saved.setRawS3Key("videos/raw/key");
            when(ingestionJobRepository.save(any(IngestionJob.class))).thenReturn(saved);
            ConfirmUploadRequest request = confirmUploadRequest("videos/raw/key", "video/mp4");

            IngestionStatusResponse response = ingestionService.confirmUpload(VIDEO_ASSET_ID, request);

            assertThat(response.getJobStatus()).isEqualTo(UPLOADED);
            ArgumentCaptor<IngestionJob> jobCaptor = ArgumentCaptor.forClass(IngestionJob.class);
            verify(ingestionJobRepository).save(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getRawS3Key()).isEqualTo("videos/raw/key");
            verify(kafkaProducerService).sendIngestionEvent(
                    eq(VIDEO_ASSET_ID), eq("videos/raw/key"), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getIngestionStatus")
    class GetIngestionStatus {

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.existsById(VIDEO_ASSET_ID)).thenReturn(false);

            assertThatThrownBy(() -> ingestionService.getIngestionStatus(VIDEO_ASSET_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("returns PENDING when no job exists")
        void returnsPendingWhenNoJob() {
            when(videoAssetRepository.existsById(VIDEO_ASSET_ID)).thenReturn(true);
            when(ingestionJobRepository.findFirstByVideoAssetIdOrderByCreatedAtDesc(VIDEO_ASSET_ID))
                    .thenReturn(Optional.empty());

            IngestionStatusResponse response = ingestionService.getIngestionStatus(VIDEO_ASSET_ID);

            assertThat(response.getVideoAssetId()).isEqualTo(VIDEO_ASSET_ID);
            assertThat(response.getJobStatus()).isEqualTo(PENDING);
            assertThat(response.getJobId()).isNull();
        }
    }
}
