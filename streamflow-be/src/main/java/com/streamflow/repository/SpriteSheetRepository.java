package com.streamflow.repository;

import com.streamflow.entity.SpriteSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for SpriteSheet entity. A video asset can have multiple sprite
 * sheets; use findAllByVideoAssetIdOrderBySheetIndexAsc for ordered list.
 */
@Repository
public interface SpriteSheetRepository extends JpaRepository<SpriteSheet, UUID> {

    List<SpriteSheet> findAllByVideoAssetIdOrderBySheetIndexAsc(UUID videoAssetId);

    boolean existsByVideoAssetId(UUID videoAssetId);
}
