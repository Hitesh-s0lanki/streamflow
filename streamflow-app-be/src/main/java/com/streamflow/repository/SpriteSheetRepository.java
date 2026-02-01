package com.streamflow.repository;

import com.streamflow.entity.SpriteSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpriteSheetRepository extends JpaRepository<SpriteSheet, UUID> {

    List<SpriteSheet> findByVideoAssetIdOrderByStartTimeSecondsAsc(UUID videoAssetId);

    @Query("SELECT s FROM SpriteSheet s WHERE s.videoAsset.id = :videoAssetId AND :timeSeconds >= s.startTimeSeconds AND :timeSeconds < s.endTimeSeconds")
    Optional<SpriteSheet> findSheetCoveringTime(@Param("videoAssetId") UUID videoAssetId,
            @Param("timeSeconds") int timeSeconds);

    @Query("SELECT s FROM SpriteSheet s LEFT JOIN FETCH s.frameMetadata WHERE s.videoAsset.id = :videoAssetId ORDER BY s.startTimeSeconds")
    List<SpriteSheet> findByVideoAssetIdWithFrameMetadata(@Param("videoAssetId") UUID videoAssetId);
}
