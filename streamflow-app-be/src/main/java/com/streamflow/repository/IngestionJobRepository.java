package com.streamflow.repository;

import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.enums.IngestionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {

    List<IngestionJob> findByVideoAssetIdOrderByCreatedAtDesc(UUID videoAssetId, Pageable pageable);

    Optional<IngestionJob> findFirstByVideoAssetIdAndJobStatusInOrderByCreatedAtDesc(UUID videoAssetId,
            List<IngestionStatus> statuses);

    List<IngestionJob> findByJobStatusOrderByCreatedAtDesc(IngestionStatus jobStatus, Pageable pageable);

    Page<IngestionJob> findByVideoAssetId(UUID videoAssetId, Pageable pageable);

    boolean existsByVideoAssetIdAndJobStatus(UUID videoAssetId, IngestionStatus jobStatus);

    Optional<IngestionJob> findFirstByVideoAssetIdOrderByCreatedAtDesc(UUID videoAssetId);

    /** Count distinct video assets with given job status (e.g. READY for ingestion readiness). */
    @Query("SELECT COUNT(DISTINCT j.videoAsset.id) FROM IngestionJob j WHERE j.jobStatus = :status")
    long countDistinctVideoAssetIdsByJobStatus(@Param("status") IngestionStatus status);

    /** Admin list: filters jobStatus, videoAssetId, contentId, date range. Order: createdAt DESC. */
    @Query("SELECT j FROM IngestionJob j JOIN j.videoAsset v WHERE (:jobStatus IS NULL OR j.jobStatus = :jobStatus)"
            + " AND (:videoAssetId IS NULL OR v.id = :videoAssetId)"
            + " AND (:contentId IS NULL OR v.content.id = :contentId)"
            + " AND (:from IS NULL OR j.createdAt >= :from) AND (:to IS NULL OR j.createdAt <= :to)"
            + " ORDER BY j.createdAt DESC")
    Page<IngestionJob> findAdminList(@Param("jobStatus") IngestionStatus jobStatus,
            @Param("videoAssetId") UUID videoAssetId,
            @Param("contentId") UUID contentId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
