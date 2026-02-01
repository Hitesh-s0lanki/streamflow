package com.streamflow.repository;

import com.streamflow.entity.PlaybackAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackAnalyticsRepository extends JpaRepository<PlaybackAnalytics, UUID> {

    List<PlaybackAnalytics> findByVideoAssetIdOrderByPeriodStartDesc(UUID videoAssetId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT pa FROM PlaybackAnalytics pa WHERE pa.videoAsset.id = :videoAssetId AND pa.periodStart <= :time AND pa.periodEnd >= :time")
    Optional<PlaybackAnalytics> findByVideoAssetIdAndPeriodContaining(@Param("videoAssetId") UUID videoAssetId,
            @Param("time") Instant time);

    List<PlaybackAnalytics> findByVideoAssetIdAndPeriodStartBetweenOrderByPeriodStartAsc(UUID videoAssetId,
            Instant from, Instant to);
}
