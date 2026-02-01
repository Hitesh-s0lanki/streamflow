package com.streamflow.repository;

import com.streamflow.entity.VideoAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {

    List<VideoAsset> findByContentId(UUID contentId);

    Optional<VideoAsset> findByEpisodeId(UUID episodeId);

    @Query("SELECT v FROM VideoAsset v LEFT JOIN FETCH v.variants WHERE v.id = :id")
    Optional<VideoAsset> findByIdWithVariants(@Param("id") UUID id);

    @Query("SELECT v FROM VideoAsset v LEFT JOIN FETCH v.spriteSheets WHERE v.id = :id")
    Optional<VideoAsset> findByIdWithSpriteSheets(@Param("id") UUID id);

    @Query("SELECT v FROM VideoAsset v LEFT JOIN FETCH v.variants LEFT JOIN FETCH v.spriteSheets WHERE v.id = :id")
    Optional<VideoAsset> findByIdWithVariantsAndSpriteSheets(@Param("id") UUID id);

    boolean existsByContentId(UUID contentId);

    boolean existsByEpisodeId(UUID episodeId);
}
