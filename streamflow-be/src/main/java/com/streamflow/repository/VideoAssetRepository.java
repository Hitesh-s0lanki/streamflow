package com.streamflow.repository;

import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VideoAsset entity operations.
 */
@Repository
public interface VideoAssetRepository extends JpaRepository<VideoAsset, UUID> {

    /**
     * Find video asset by content ID.
     */
    Optional<VideoAsset> findByContentId(UUID contentId);

    /**
     * Check if a video asset exists for a given content.
     */
    boolean existsByContentId(UUID contentId);

    /**
     * Find all video assets with a specific upload status.
     */
    List<VideoAsset> findByUploadStatus(UploadStatus status);

    /**
     * Find video assets with failed uploads.
     */
    List<VideoAsset> findByUploadStatusOrderByUploadCompletedAtDesc(UploadStatus status);

    /**
     * Find stale multipart uploads (initiated but not completed within threshold).
     */
    @Query("SELECT v FROM VideoAsset v WHERE v.uploadStatus = :status AND v.uploadStartedAt < :threshold")
    List<VideoAsset> findStaleMultipartUploads(
            @Param("status") UploadStatus status,
            @Param("threshold") Instant threshold);

    /**
     * Find video asset by multipart upload ID.
     */
    Optional<VideoAsset> findByMultipartUploadId(String multipartUploadId);

    /**
     * Find video asset by raw S3 key.
     */
    Optional<VideoAsset> findByRawS3Key(String rawS3Key);

    /**
     * Atomically update uploaded parts count. Thread-safe for concurrent
     * multipart upload threads — each runs in its own short transaction.
     */
    @Modifying
    @Transactional
    @Query("UPDATE VideoAsset v SET v.uploadedParts = :uploadedParts WHERE v.id = :id")
    void updateUploadedParts(@Param("id") UUID id, @Param("uploadedParts") int uploadedParts);
}
