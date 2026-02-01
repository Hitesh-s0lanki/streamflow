package com.streamflow.service;

import com.streamflow.entity.SpriteFrameMetadata;
import com.streamflow.entity.SpriteSheet;
import com.streamflow.repository.SpriteFrameMetadataRepository;
import com.streamflow.repository.SpriteSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpriteFrameMetadataService {

    private final SpriteFrameMetadataRepository spriteFrameMetadataRepository;
    private final SpriteSheetRepository spriteSheetRepository;

    @Transactional(readOnly = true)
    public Optional<SpriteFrameMetadata> findById(UUID id) {
        return spriteFrameMetadataRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<SpriteFrameMetadata> findBySpriteSheetId(UUID spriteSheetId) {
        return spriteFrameMetadataRepository.findBySpriteSheetIdOrderByFrameIndexAsc(spriteSheetId);
    }

    @Transactional(readOnly = true)
    public Optional<SpriteFrameMetadata> findBySpriteSheetIdAndTimeOffset(UUID spriteSheetId,
            Integer timeOffsetSeconds) {
        return spriteFrameMetadataRepository.findBySpriteSheetIdAndTimeOffsetSeconds(spriteSheetId, timeOffsetSeconds);
    }

    @Transactional
    public SpriteFrameMetadata create(UUID spriteSheetId, int frameIndex, int timeOffsetSeconds,
            Integer xPosition, Integer yPosition, Integer width, Integer height) {
        SpriteSheet sheet = spriteSheetRepository.findById(spriteSheetId)
                .orElseThrow(() -> new IllegalArgumentException("SpriteSheet not found: " + spriteSheetId));
        SpriteFrameMetadata meta = new SpriteFrameMetadata();
        meta.setSpriteSheet(sheet);
        meta.setFrameIndex(frameIndex);
        meta.setTimeOffsetSeconds(timeOffsetSeconds);
        meta.setXPosition(xPosition);
        meta.setYPosition(yPosition);
        meta.setWidth(width);
        meta.setHeight(height);
        return spriteFrameMetadataRepository.save(meta);
    }

    @Transactional
    public SpriteFrameMetadata save(SpriteFrameMetadata metadata) {
        return spriteFrameMetadataRepository.save(metadata);
    }

    @Transactional
    public void deleteById(UUID id) {
        spriteFrameMetadataRepository.deleteById(id);
    }
}
