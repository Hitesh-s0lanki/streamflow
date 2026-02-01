package com.streamflow.service;

import com.streamflow.entity.SpriteSheet;
import com.streamflow.entity.VideoAsset;
import com.streamflow.repository.SpriteSheetRepository;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpriteSheetService {

    private final SpriteSheetRepository spriteSheetRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<SpriteSheet> findById(UUID id) {
        return spriteSheetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<SpriteSheet> findByVideoAssetId(UUID videoAssetId) {
        return spriteSheetRepository.findByVideoAssetIdOrderByStartTimeSecondsAsc(videoAssetId);
    }

    @Transactional(readOnly = true)
    public Optional<SpriteSheet> findSheetCoveringTime(UUID videoAssetId, int timeSeconds) {
        return spriteSheetRepository.findSheetCoveringTime(videoAssetId, timeSeconds);
    }

    @Transactional(readOnly = true)
    public List<SpriteSheet> findByVideoAssetIdWithFrameMetadata(UUID videoAssetId) {
        return spriteSheetRepository.findByVideoAssetIdWithFrameMetadata(videoAssetId);
    }

    @Transactional
    public SpriteSheet create(UUID videoAssetId, String spriteUrl, int startTimeSeconds, int endTimeSeconds,
            int columns, int rows, int thumbnailWidth, int thumbnailHeight, Integer intervalSeconds) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        SpriteSheet sheet = new SpriteSheet();
        sheet.setVideoAsset(asset);
        sheet.setSpriteUrl(spriteUrl);
        sheet.setStartTimeSeconds(startTimeSeconds);
        sheet.setEndTimeSeconds(endTimeSeconds);
        sheet.setColumns(columns);
        sheet.setRows(rows);
        sheet.setThumbnailWidth(thumbnailWidth);
        sheet.setThumbnailHeight(thumbnailHeight);
        sheet.setIntervalSeconds(intervalSeconds);
        return spriteSheetRepository.save(sheet);
    }

    @Transactional
    public SpriteSheet save(SpriteSheet spriteSheet) {
        return spriteSheetRepository.save(spriteSheet);
    }

    @Transactional
    public void deleteById(UUID id) {
        spriteSheetRepository.deleteById(id);
    }
}
