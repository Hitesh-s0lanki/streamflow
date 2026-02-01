package com.streamflow.repository;

import com.streamflow.entity.WatchProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchProgressRepository extends JpaRepository<WatchProgress, UUID> {

    Optional<WatchProgress> findByUserIdAndVideoAssetId(String userId, UUID videoAssetId);

    List<WatchProgress> findByUserIdAndCompletedFalseOrderByLastWatchedAtDesc(String userId, Pageable pageable);

    List<WatchProgress> findByUserIdOrderByLastWatchedAtDesc(String userId, Pageable pageable);

    @Query("SELECT wp FROM WatchProgress wp JOIN FETCH wp.videoAsset WHERE wp.userId = :userId AND wp.completed = false ORDER BY wp.lastWatchedAt DESC")
    List<WatchProgress> findContinueWatching(@Param("userId") String userId, Pageable pageable);

    boolean existsByUserIdAndVideoAssetId(String userId, UUID videoAssetId);
}
