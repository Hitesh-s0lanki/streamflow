package com.streamflow.repository;

import com.streamflow.entity.VideoVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoVariantRepository extends JpaRepository<VideoVariant, UUID> {

    List<VideoVariant> findAllByVideoAssetIdOrderBySortOrderAsc(UUID videoAssetId);

    @Transactional
    void deleteAllByVideoAssetId(UUID videoAssetId);
}
