package com.streamflow.entity;

import com.streamflow.entity.enums.IngestionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Tracks ingestion lifecycle for a video upload: raw S3 key, processing state,
 * errors.
 */
@Getter
@Setter
@Entity
@Table(name = "ingestion_job", indexes = {
        @Index(name = "idx_ingestion_job_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_ingestion_job_status", columnList = "job_status"),
        @Index(name = "idx_ingestion_job_created_at", columnList = "created_at")
})
public class IngestionJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_status", nullable = false, length = 32)
    private IngestionStatus jobStatus = IngestionStatus.PENDING;

    @Column(name = "raw_s3_key", length = 1024)
    private String rawS3Key;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "processed_at")
    private java.time.Instant processedAt;
}
