package com.streamflow.repository;

import com.streamflow.entity.SpriteFrameMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpriteFrameMetadataRepository extends JpaRepository<SpriteFrameMetadata, UUID> {

    List<SpriteFrameMetadata> findBySpriteSheetIdOrderByFrameIndexAsc(UUID spriteSheetId);

    Optional<SpriteFrameMetadata> findBySpriteSheetIdAndTimeOffsetSeconds(UUID spriteSheetId,
            Integer timeOffsetSeconds);

    List<SpriteFrameMetadata> findBySpriteSheetIdOrderByTimeOffsetSecondsAsc(UUID spriteSheetId);

    boolean existsBySpriteSheetIdAndFrameIndex(UUID spriteSheetId, Integer frameIndex);
}
