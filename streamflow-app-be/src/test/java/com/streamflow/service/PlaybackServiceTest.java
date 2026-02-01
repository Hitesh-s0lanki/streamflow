package com.streamflow.service;

import com.streamflow.dto.PlaybackLicenseResponse;
import com.streamflow.entity.PlaybackLicense;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.IngestionStatus;
import com.streamflow.entity.enums.LicenseStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.IngestionJobRepository;
import com.streamflow.repository.PlaybackLicenseRepository;
import com.streamflow.repository.PlaybackEventLogRepository;
import com.streamflow.repository.SignedPlaybackUrlRepository;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.VideoVariantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.streamflow.config.TestFixtures.LICENSE_ID;
import static com.streamflow.config.TestFixtures.USER_ID;
import static com.streamflow.config.TestFixtures.VIDEO_ASSET_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackServiceTest {

    @Mock
    private PlaybackLicenseRepository playbackLicenseRepository;
    @Mock
    private SignedPlaybackUrlRepository signedPlaybackUrlRepository;
    @Mock
    private PlaybackEventLogRepository playbackEventLogRepository;
    @Mock
    private VideoAssetRepository videoAssetRepository;
    @Mock
    private VideoVariantRepository videoVariantRepository;
    @Mock
    private IngestionJobRepository ingestionJobRepository;

    @InjectMocks
    private PlaybackService playbackService;

    @Nested
    @DisplayName("requestLicense")
    class RequestLicense {

        @Test
        @DisplayName("throws when userId is blank")
        void throwsWhenUserIdBlank() {
            assertThatThrownBy(() -> playbackService.requestLicense(" ", "device", VIDEO_ASSET_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("userId");
        }

        @Test
        @DisplayName("throws when video asset not found")
        void throwsWhenVideoAssetNotFound() {
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playbackService.requestLicense(USER_ID, null, VIDEO_ASSET_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VideoAsset");
        }

        @Test
        @DisplayName("throws when ingestion not READY")
        void throwsWhenIngestionNotReady() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(ingestionJobRepository.existsByVideoAssetIdAndJobStatus(VIDEO_ASSET_ID, IngestionStatus.READY))
                    .thenReturn(false);

            assertThatThrownBy(() -> playbackService.requestLicense(USER_ID, null, VIDEO_ASSET_ID))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("READY");
        }

        @Test
        @DisplayName("returns existing active license when present")
        void returnsExistingActiveLicense() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            PlaybackLicense license = new PlaybackLicense();
            license.setId(LICENSE_ID);
            license.setUserId(USER_ID);
            license.setVideoAsset(asset);
            license.setExpiresAt(Instant.now().plusSeconds(3600));
            license.setLicenseStatus(LicenseStatus.ACTIVE);
            when(videoAssetRepository.findById(VIDEO_ASSET_ID)).thenReturn(Optional.of(asset));
            when(ingestionJobRepository.existsByVideoAssetIdAndJobStatus(VIDEO_ASSET_ID, IngestionStatus.READY))
                    .thenReturn(true);
            when(playbackLicenseRepository.findActiveLicense(eq(USER_ID), eq(VIDEO_ASSET_ID), eq(LicenseStatus.ACTIVE), any()))
                    .thenReturn(Optional.of(license));

            PlaybackLicenseResponse response = playbackService.requestLicense(USER_ID, "device", VIDEO_ASSET_ID);

            assertThat(response.getLicenseId()).isEqualTo(LICENSE_ID);
            assertThat(response.getVideoAssetId()).isEqualTo(VIDEO_ASSET_ID);
            verify(playbackLicenseRepository).findActiveLicense(any(), eq(VIDEO_ASSET_ID), eq(LicenseStatus.ACTIVE), any());
        }
    }

    @Nested
    @DisplayName("validateLicense")
    class ValidateLicense {

        @Test
        @DisplayName("throws when userId is blank")
        void throwsWhenUserIdBlank() {
            assertThatThrownBy(() -> playbackService.validateLicense(LICENSE_ID, " "))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("throws when license not found")
        void throwsWhenLicenseNotFound() {
            when(playbackLicenseRepository.findByIdAndUserId(LICENSE_ID, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> playbackService.validateLicense(LICENSE_ID, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("PlaybackLicense");
        }
    }

    @Nested
    @DisplayName("revokeLicense")
    class RevokeLicense {

        @Test
        @DisplayName("revokes license and returns response")
        void revokesLicense() {
            VideoAsset asset = new VideoAsset();
            asset.setId(VIDEO_ASSET_ID);
            PlaybackLicense license = new PlaybackLicense();
            license.setId(LICENSE_ID);
            license.setVideoAsset(asset);
            license.setLicenseStatus(LicenseStatus.ACTIVE);
            when(playbackLicenseRepository.findById(LICENSE_ID)).thenReturn(Optional.of(license));
            when(playbackLicenseRepository.save(any(PlaybackLicense.class))).thenAnswer(i -> i.getArgument(0));

            PlaybackLicenseResponse response = playbackService.revokeLicense(LICENSE_ID);

            assertThat(response.getLicenseStatus()).isEqualTo(LicenseStatus.REVOKED);
            verify(playbackLicenseRepository).save(license);
            assertThat(license.getLicenseStatus()).isEqualTo(LicenseStatus.REVOKED);
        }
    }
}
