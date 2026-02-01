package com.streamflow.repository;

import com.streamflow.entity.PlaybackLicense;
import com.streamflow.entity.enums.LicenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackLicenseRepository extends JpaRepository<PlaybackLicense, UUID> {

    List<PlaybackLicense> findByUserIdOrderByCreatedAtDesc(String userId,
            org.springframework.data.domain.Pageable pageable);

    List<PlaybackLicense> findByVideoAssetIdAndUserIdAndLicenseStatus(UUID videoAssetId, String userId,
            LicenseStatus licenseStatus);

    @Query("SELECT pl FROM PlaybackLicense pl WHERE pl.userId = :userId AND pl.videoAsset.id = :videoAssetId AND pl.licenseStatus = :licenseStatus AND pl.expiresAt > :now ORDER BY pl.expiresAt DESC")
    Optional<PlaybackLicense> findActiveLicense(@Param("userId") String userId,
            @Param("videoAssetId") UUID videoAssetId, @Param("licenseStatus") LicenseStatus licenseStatus,
            @Param("now") Instant now);

    boolean existsByUserIdAndVideoAssetIdAndLicenseStatusAndExpiresAtAfter(String userId, UUID videoAssetId,
            LicenseStatus licenseStatus, Instant now);
}
