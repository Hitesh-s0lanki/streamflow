package com.streamflow.repository;

import com.streamflow.entity.IngestionJob;
import com.streamflow.entity.enums.IngestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
