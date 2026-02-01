package com.streamflow.repository;

import com.streamflow.entity.SeriesSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeriesSeasonRepository extends JpaRepository<SeriesSeason, UUID> {

    List<SeriesSeason> findByContentIdOrderBySeasonNumberAsc(UUID contentId);

    @Query("SELECT s FROM SeriesSeason s JOIN FETCH s.episodes WHERE s.content.id = :contentId ORDER BY s.seasonNumber")
    List<SeriesSeason> findByContentIdWithEpisodes(@Param("contentId") UUID contentId);

    Optional<SeriesSeason> findByContentIdAndSeasonNumber(UUID contentId, Integer seasonNumber);

    boolean existsByContentIdAndSeasonNumber(UUID contentId, Integer seasonNumber);
}
