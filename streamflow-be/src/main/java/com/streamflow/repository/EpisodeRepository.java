package com.streamflow.repository;

import com.streamflow.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findBySeasonIdOrderByEpisodeNumberAsc(UUID seasonId);

    Optional<Episode> findBySeasonIdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);

    Optional<Episode> findByVideoAssetId(UUID videoAssetId);

    @Query("SELECT e FROM Episode e JOIN FETCH e.videoAsset WHERE e.season.id = :seasonId ORDER BY e.episodeNumber")
    List<Episode> findBySeasonIdWithVideoAsset(@Param("seasonId") UUID seasonId);

    boolean existsBySeasonIdAndEpisodeNumber(UUID seasonId, Integer episodeNumber);
}
