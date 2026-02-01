package com.streamflow.repository;

import com.streamflow.entity.VideoVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VideoVariantRepository extends JpaRepository<VideoVariant, UUID> {

    List<VideoVariant> findByVideoAssetIdOrderBySortOrderAsc(UUID videoAssetId);

    List<VideoVariant> findByVideoAssetIdOrderByBitrateKbpsAsc(UUID videoAssetId);

    boolean existsByVideoAssetIdAndResolution(UUID videoAssetId, String resolution);

    boolean existsByVideoAssetIdAndSortOrder(UUID videoAssetId, Integer sortOrder);
}
