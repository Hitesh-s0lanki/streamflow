package com.streamflow.entity;

import com.streamflow.entity.enums.ProcessingStatus;
import com.streamflow.entity.enums.UploadStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Video asset entity representing a video file associated with content (movie or episode).
 * Tracks the entire upload lifecycle including multipart uploads.
 */
@Getter
@Setter
@Entity
@Table(name = "video_asset", indexes = {
        @Index(name = "idx_video_asset_content_id", columnList = "content_id"),
        @Index(name = "idx_video_asset_episode_id", columnList = "episode_id", unique = true),
        @Index(name = "idx_video_asset_upload_status", columnList = "upload_status")
})
public class VideoAsset extends BaseEntity {

    /**
     * Content for the video asset (for MOVIE content type).
     * One content can have one video asset.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", unique = true)
    private Content content;

    /** Duration in seconds (populated after upload/processing). Defaults to 0 until known. */
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds = 0;

    /** Original filename of the uploaded video. */
    @Column(name = "original_filename", length = 512)
    private String originalFilename;

    /** Content type/MIME type of the uploaded video. */
    @Column(name = "content_type", length = 128)
    private String contentType;

    /** Size of the video file in bytes. */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** S3 key for the raw uploaded video file. */
    @Column(name = "raw_s3_key", length = 1024)
    private String rawS3Key;

    /** S3 multipart upload ID (for tracking in-progress multipart uploads). */
    @Column(name = "multipart_upload_id", length = 256)
    private String multipartUploadId;

    /** Current upload status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 32)
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    /** Error message if upload failed. */
    @Column(name = "upload_error_message", columnDefinition = "TEXT")
    private String uploadErrorMessage;

    /** Timestamp when upload started. */
    @Column(name = "upload_started_at")
    private Instant uploadStartedAt;

    /** Timestamp when upload completed (success or failure). */
    @Column(name = "upload_completed_at")
    private Instant uploadCompletedAt;

    /** URL to the HLS/DASH manifest (populated after transcoding). */
    @Column(name = "manifest_url", length = 1024)
    private String manifestUrl;

    /** Whether DRM is enabled for this video. */
    @Column(name = "drm_enabled", nullable = false)
    private Boolean drmEnabled = false;

    /** Current processing status (transcoding, sprite generation, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32, columnDefinition = "VARCHAR(32) DEFAULT 'NONE' NOT NULL")
    private ProcessingStatus processingStatus = ProcessingStatus.NONE;

    /** Error message if processing failed. */
    @Column(name = "processing_error_message", columnDefinition = "TEXT")
    private String processingErrorMessage;

    /** Timestamp when processing started. */
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    /** Timestamp when processing completed (success or failure). */
    @Column(name = "processing_completed_at")
    private Instant processingCompletedAt;

    /** Number of parts in multipart upload (for tracking progress). */
    @Column(name = "total_parts")
    private Integer totalParts;

    /** Number of parts successfully uploaded. */
    @Column(name = "uploaded_parts")
    private Integer uploadedParts;

    @OneToMany(mappedBy = "videoAsset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VideoVariant> variants = new ArrayList<>();

    /**
     * Marks the upload as started.
     */
    public void markUploadStarted() {
        this.uploadStatus = UploadStatus.UPLOADING;
        this.uploadStartedAt = Instant.now();
        this.uploadErrorMessage = null;
    }

    /**
     * Marks the upload as completed successfully.
     */
    public void markUploadCompleted(String rawS3Key) {
        this.uploadStatus = UploadStatus.COMPLETED;
        this.uploadCompletedAt = Instant.now();
        this.rawS3Key = rawS3Key;
        this.uploadErrorMessage = null;
    }

    /**
     * Marks the upload as failed with an error message.
     */
    public void markUploadFailed(String errorMessage) {
        this.uploadStatus = UploadStatus.FAILED;
        this.uploadCompletedAt = Instant.now();
        this.uploadErrorMessage = errorMessage;
    }

    /**
     * Marks the multipart upload as initiated.
     */
    public void markMultipartInitiated(String uploadId, int totalParts) {
        this.uploadStatus = UploadStatus.MULTIPART_INITIATED;
        this.multipartUploadId = uploadId;
        this.totalParts = totalParts;
        this.uploadedParts = 0;
        this.uploadStartedAt = Instant.now();
    }

    /**
     * Increments the count of successfully uploaded parts.
     */
    public void incrementUploadedParts() {
        if (this.uploadedParts == null) {
            this.uploadedParts = 0;
        }
        this.uploadedParts++;
    }

    /**
     * Marks the upload as cancelled/aborted.
     */
    public void markCancelled() {
        this.uploadStatus = UploadStatus.CANCELLED;
        this.uploadCompletedAt = Instant.now();
    }

    public void markProcessingQueued() {
        this.processingStatus = ProcessingStatus.QUEUED;
        this.processingErrorMessage = null;
    }

    public void markProcessingStarted() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.processingStartedAt = Instant.now();
        this.processingErrorMessage = null;
    }

    public void markProcessingCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processingCompletedAt = Instant.now();
        this.processingErrorMessage = null;
    }

    public void markProcessingFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingCompletedAt = Instant.now();
        this.processingErrorMessage = errorMessage;
    }
}
