package com.streamflow.repository;

import com.streamflow.entity.PlaybackEventLog;
import com.streamflow.entity.enums.PlaybackEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlaybackEventLogRepository extends JpaRepository<PlaybackEventLog, UUID> {

    Page<PlaybackEventLog> findByVideoAssetId(UUID videoAssetId, Pageable pageable);

    Page<PlaybackEventLog> findByUserId(String userId, Pageable pageable);

    List<PlaybackEventLog> findByVideoAssetIdAndEventTypeAndCreatedAtBetween(UUID videoAssetId,
            PlaybackEventType eventType, Instant from, Instant to);
}
