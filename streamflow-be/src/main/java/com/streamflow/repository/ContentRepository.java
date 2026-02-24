package com.streamflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.streamflow.entity.Content;
import com.streamflow.entity.enums.PublishStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/* Content repository */
@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    List<Content> findByPublishStatusOrderByCreatedAtDesc(PublishStatus publishStatus);

    List<Content> findByPublishStatus(PublishStatus status, Pageable pageable);

    /**
     * Fetches content with its video asset in one query for playback session creation.
     */
    @Query("SELECT c FROM Content c LEFT JOIN FETCH c.videoAsset WHERE c.id = :id")
    Optional<Content> findByIdWithVideoAsset(@Param("id") UUID id);
}
