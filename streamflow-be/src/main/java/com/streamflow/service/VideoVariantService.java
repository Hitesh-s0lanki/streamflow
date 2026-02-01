package com.streamflow.service;

import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.VideoVariant;
import com.streamflow.repository.VideoAssetRepository;
import com.streamflow.repository.VideoVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoVariantService {

    private final VideoVariantRepository videoVariantRepository;
    private final VideoAssetRepository videoAssetRepository;

    @Transactional(readOnly = true)
    public List<VideoVariant> findByVideoAssetId(UUID videoAssetId) {
        return videoVariantRepository.findByVideoAssetIdOrderBySortOrderAsc(videoAssetId);
    }

    @Transactional(readOnly = true)
    public Optional<VideoVariant> findById(UUID id) {
        return videoVariantRepository.findById(id);
    }

    @Transactional
    public VideoVariant addVariant(UUID videoAssetId, String resolution, Integer bitrateKbps, String codec,
            String segmentPath, Integer sortOrder) {
        VideoAsset asset = videoAssetRepository.findById(videoAssetId)
                .orElseThrow(() -> new IllegalArgumentException("VideoAsset not found: " + videoAssetId));
        if (videoVariantRepository.existsByVideoAssetIdAndResolution(videoAssetId, resolution)) {
            throw new IllegalArgumentException("Variant with resolution " + resolution + " already exists");
        }
        VideoVariant variant = new VideoVariant();
        variant.setVideoAsset(asset);
        variant.setResolution(resolution);
        variant.setBitrateKbps(bitrateKbps);
        variant.setCodec(codec);
        variant.setSegmentPath(segmentPath);
        variant.setSortOrder(sortOrder != null ? sortOrder : 0);
        return videoVariantRepository.save(variant);
    }

    @Transactional
    public VideoVariant save(VideoVariant variant) {
        return videoVariantRepository.save(variant);
    }

    @Transactional
    public void deleteById(UUID id) {
        videoVariantRepository.deleteById(id);
    }
}
