package com.streamflow.repository;

import com.streamflow.entity.PlaybackEventLog;
import com.streamflow.entity.enums.PlaybackEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlaybackEventLogRepository extends JpaRepository<PlaybackEventLog, UUID> {

        Page<PlaybackEventLog> findByVideoAssetId(UUID videoAssetId, Pageable pageable);

        Page<PlaybackEventLog> findByUserId(String userId, Pageable pageable);

        List<PlaybackEventLog> findByVideoAssetIdAndEventTypeAndCreatedAtBetween(UUID videoAssetId,
                        PlaybackEventType eventType, Instant from, Instant to);

        /**
         * Admin list: filters userId, videoAssetId, eventType, date range. Order:
         * createdAt DESC (newest first).
         */
        @Query("SELECT e FROM PlaybackEventLog e WHERE (:userId IS NULL OR e.userId = :userId)"
                        + " AND (:videoAssetId IS NULL OR e.videoAsset.id = :videoAssetId)"
                        + " AND (:eventType IS NULL OR e.eventType = :eventType)"
                        + " AND (:from IS NULL OR e.createdAt >= :from) AND (:to IS NULL OR e.createdAt <= :to)"
                        + " ORDER BY e.createdAt DESC")
        Page<PlaybackEventLog> findAdminList(@Param("userId") String userId,
                        @Param("videoAssetId") UUID videoAssetId,
                        @Param("eventType") PlaybackEventType eventType,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);
}
