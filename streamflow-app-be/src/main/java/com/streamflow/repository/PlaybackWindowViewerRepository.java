package com.streamflow.repository;

import com.streamflow.entity.PlaybackWindowViewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PlaybackWindowViewerRepository extends JpaRepository<PlaybackWindowViewer, UUID> {

    Optional<PlaybackWindowViewer> findByVideoAssetIdAndPeriodStartAndUserId(UUID videoAssetId, Instant periodStart,
            String userId);

    long countByVideoAssetIdAndPeriodStart(UUID videoAssetId, Instant periodStart);

    /** Delete viewers in time range (for admin rebuild). */
    void deleteByPeriodStartBetween(Instant from, Instant to);
}
