package com.streamflow.service;

import com.streamflow.entity.VideoAsset;
import com.streamflow.repository.VideoAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoAssetService {

    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public Optional<VideoAsset> findById(UUID id) {
        return videoAssetRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<VideoAsset> findByIdWithVariants(UUID id) {
        return videoAssetRepository.findByIdWithVariants(id);
    }

    @Transactional(readOnly = true)
    public Optional<VideoAsset> findByIdWithSpriteSheets(UUID id) {
        return videoAssetRepository.findByIdWithSpriteSheets(id);
    }

    @Transactional(readOnly = true)
    public Optional<VideoAsset> findByIdWithVariantsAndSpriteSheets(UUID id) {
        return videoAssetRepository.findByIdWithVariantsAndSpriteSheets(id);
    }

    @Transactional(readOnly = true)
    public List<VideoAsset> findByContentId(UUID contentId) {
        return videoAssetRepository.findByContentId(contentId);
    }

    @Transactional(readOnly = true)
    public Optional<VideoAsset> findByEpisodeId(UUID episodeId) {
        return videoAssetRepository.findByEpisodeId(episodeId);
    }

    @Transactional
    public VideoAsset save(VideoAsset videoAsset) {
        return videoAssetRepository.save(videoAsset);
    }

    @Transactional
    public void deleteById(UUID id) {
        videoAssetRepository.deleteById(id);
    }
}
