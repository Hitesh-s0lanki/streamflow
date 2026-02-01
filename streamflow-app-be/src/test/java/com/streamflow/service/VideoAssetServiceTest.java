package com.streamflow.service;

import com.streamflow.dto.CreateVideoAssetRequest;
import com.streamflow.dto.UploadUrlResponse;
import com.streamflow.dto.VideoAssetResponse;
import com.streamflow.entity.Content;
import com.streamflow.entity.Episode;
import com.streamflow.entity.SeriesSeason;
import com.streamflow.entity.VideoAsset;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.streamflow.config.TestFixtures.CONTENT_ID;
import static com.streamflow.config.TestFixtures.EPISODE_ID;
import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static com.streamflow.entity.enums.ContentType.MOVIE;
import static com.streamflow.entity.enums.ContentType.SERIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoAssetServiceTest {

    @Mock
    private VideoAssetRepository videoAssetRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private IngestionJobRepository ingestionJobRepository;
    @Mock
    private VideoVariantRepository videoVariantRepository;
    @Mock
    private SpriteSheetRepository spriteSheetRepository;
    @Mock
    private SpriteFrameMetadataRepository spriteFrameMetadataRepository;
    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private VideoAssetService videoAssetService;

    @Nested
    @DisplayName("createVideoAsset")
    class CreateVideoAsset {

        @Test
        @DisplayName("throws when both contentId and episodeId set")
        void throwsWhenBothSet() {
            CreateVideoAssetRequest request = new CreateVideoAssetRequest();
            request.setContentId(CONTENT_ID);
            request.setEpisodeId(EPISODE_ID);
            request.setDurationSeconds(120);

            assertThatThrownBy(() -> videoAssetService.createVideoAsset(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Exactly one");
        }

        @Test
        @DisplayName("throws when content not found")
        void throwsWhenContentNotFound() {
            CreateVideoAssetRequest request = new CreateVideoAssetRequest();
            request.setContentId(CONTENT_ID);
            request.setEpisodeId(null);
            request.setDurationSeconds(120);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> videoAssetService.createVideoAsset(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Content");
        }

        @Test
        @DisplayName("throws when content is not MOVIE for contentId")
        void throwsWhenContentNotMovie() {
            Content content = new Content();
            content.setId(CONTENT_ID);
            content.setContentType(SERIES);
            CreateVideoAssetRequest request = new CreateVideoAssetRequest();
            request.setContentId(CONTENT_ID);
            request.setEpisodeId(null);
            request.setDurationSeconds(120);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));

            assertThatThrownBy(() -> videoAssetService.createVideoAsset(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("MOVIE");
        }

        @Test
        @DisplayName("creates video asset for movie and returns response")
        void createsVideoAssetForMovie() {
            Content content = new Content();
            content.setId(CONTENT_ID);
            content.setContentType(MOVIE);
            CreateVideoAssetRequest request = new CreateVideoAssetRequest();
            request.setContentId(CONTENT_ID);
            request.setEpisodeId(null);
            request.setDurationSeconds(120);
            when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
            when(videoAssetRepository.existsByContentId(CONTENT_ID)).thenReturn(false);
            VideoAsset saved = new VideoAsset();
            saved.setId(VIDEO_ASSET_ID);
            saved.setContent(content);
            saved.setDurationSeconds(120);
            when(videoAssetRepository.save(any(VideoAsset.class))).thenReturn(saved);

            VideoAssetResponse response = videoAssetService.createVideoAsset(request);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(VIDEO_ASSET_ID);
            assertThat(response.getContentId()).isEqualTo(CONTENT_ID);
            assertThat(response.getDurationSeconds()).isEqualTo(120);
            verify(videoAssetRepository).save(any(VideoAsset.class));
        }

        @Test
        @DisplayName("throws when episode not found for episodeId")
        void throwsWhenEpisodeNotFound() {
            CreateVideoAssetRequest request = new CreateVideoAssetRequest();
            request.setContentId(null);
            request.setEpisodeId(EPISODE_ID);
            request.setDurationSeconds(120);
            when(episodeRepository.findById(EPISODE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> videoAssetService.createVideoAsset(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Episode");
        }
    }

    @Nested
    @DisplayName("getUploadUrl")
    class GetUploadUrl {

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> videoAssetService.getUploadUrl(VIDEO_ASSET_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VideoAsset");
        }

        @Test
        @DisplayName("returns presigned URL when S3 configured")
        void returnsPresignedUrlWhenS3Configured() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            UploadUrlResponse urlResponse = UploadUrlResponse.builder()
                    .uploadUrl("https://s3.example.com/upload")
                    .rawS3Key("videos/raw/key")
                    .expiration(java.time.Instant.now().plusSeconds(900))
                    .build();
            when(s3StorageService.generateRawVideoKey(VIDEO_ASSET_ID)).thenReturn("videos/raw/key");
            when(s3StorageService.generatePresignedPutUrl("videos/raw/key", 15)).thenReturn(urlResponse);

            UploadUrlResponse response = videoAssetService.getUploadUrl(VIDEO_ASSET_ID);

            assertThat(response.getUploadUrl()).isEqualTo("https://s3.example.com/upload");
            assertThat(response.getRawS3Key()).isEqualTo("videos/raw/key");
        }

        @Test
        @DisplayName("throws when S3 not configured")
        void throwsWhenS3NotConfigured() {
            VideoAssetService serviceNoS3 = new VideoAssetService(
                    videoAssetRepository, contentRepository, episodeRepository,
                    ingestionJobRepository, videoVariantRepository, spriteSheetRepository,
                    spriteFrameMetadataRepository, null);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(new VideoAsset()));

            assertThatThrownBy(() -> serviceNoS3.getUploadUrl(VIDEO_ASSET_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("S3");
        }
    }
}
