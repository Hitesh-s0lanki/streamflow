package com.streamflow.repository;

import com.streamflow.entity.PlaybackAnalytics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackAnalyticsRepository extends JpaRepository<PlaybackAnalytics, UUID> {

        List<PlaybackAnalytics> findByVideoAssetIdOrderByPeriodStartDesc(UUID videoAssetId,
                        Pageable pageable);

        List<PlaybackAnalytics> findByVideoAssetIdAndPeriodStartBetween(UUID videoAssetId, Instant from, Instant to,
                        Pageable pageable);

        @Query("SELECT pa FROM PlaybackAnalytics pa WHERE pa.videoAsset.id = :videoAssetId AND pa.periodStart <= :time AND pa.periodEnd >= :time")
        Optional<PlaybackAnalytics> findByVideoAssetIdAndPeriodContaining(@Param("videoAssetId") UUID videoAssetId,
                        @Param("time") Instant time);

        List<PlaybackAnalytics> findByVideoAssetIdAndPeriodStartBetweenOrderByPeriodStartAsc(UUID videoAssetId,
                        Instant from, Instant to);

        /** For upsert: find analytics row by video and period (unique window). */
        Optional<PlaybackAnalytics> findByVideoAssetIdAndPeriodStart(UUID videoAssetId, Instant periodStart);

        @Query("SELECT COALESCE(SUM(pa.totalPlays), 0) FROM PlaybackAnalytics pa WHERE (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to)")
        long sumTotalPlays(@Param("from") Instant from, @Param("to") Instant to);

        @Query("SELECT COALESCE(SUM(pa.uniqueViewers), 0) FROM PlaybackAnalytics pa WHERE (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to)")
        long sumUniqueViewers(@Param("from") Instant from, @Param("to") Instant to);

        @Query("SELECT AVG(pa.completionRate) FROM PlaybackAnalytics pa WHERE pa.completionRate IS NOT NULL AND (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to)")
        java.math.BigDecimal avgCompletionRate(@Param("from") Instant from, @Param("to") Instant to);

        @Query("SELECT AVG(pa.bufferingRate) FROM PlaybackAnalytics pa WHERE pa.bufferingRate IS NOT NULL AND (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to)")
        java.math.BigDecimal avgBufferingRate(@Param("from") Instant from, @Param("to") Instant to);

        @Query("SELECT pa.videoAsset.id FROM PlaybackAnalytics pa WHERE (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to) GROUP BY pa.videoAsset.id ORDER BY SUM(pa.totalPlays) DESC")
        List<UUID> findTopVideoAssetIdsByTotalPlays(@Param("from") Instant from, @Param("to") Instant to,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(pa.totalPlays), 0) FROM PlaybackAnalytics pa WHERE pa.videoAsset.id = :videoAssetId AND (:from IS NULL OR pa.periodStart >= :from) AND (:to IS NULL OR pa.periodEnd <= :to)")
        long sumTotalPlaysByVideoAssetId(@Param("videoAssetId") UUID videoAssetId, @Param("from") Instant from,
                        @Param("to") Instant to);

        /** Delete analytics in time range (for admin rebuild). */
        void deleteByPeriodStartGreaterThanEqualAndPeriodEndLessThanEqual(Instant from, Instant to);
}
