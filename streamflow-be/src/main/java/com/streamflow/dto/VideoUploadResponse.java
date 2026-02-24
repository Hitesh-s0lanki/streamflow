package com.streamflow.dto;

import com.streamflow.entity.enums.UploadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for video upload operations.
 */
@Data
@Builder
public class VideoUploadResponse {

    /** ID of the content the video belongs to */
    private UUID contentId;

    /** ID of the created/updated video asset */
    private UUID videoAssetId;

    /** Current upload status */
    private UploadStatus uploadStatus;

    /** Original filename of the uploaded video */
    private String originalFilename;

    /** Size of the uploaded file in bytes */
    private Long fileSizeBytes;

    /** S3 key where the raw video is stored */
    private String rawS3Key;

    /** Timestamp when upload started */
    private Instant uploadStartedAt;

    /** Timestamp when upload completed */
    private Instant uploadCompletedAt;

    /** Error message if upload failed */
    private String errorMessage;

    /** Number of parts for multipart upload (null for direct upload) */
    private Integer totalParts;

    /** Number of parts successfully uploaded */
    private Integer uploadedParts;

    /** Upload progress as a percentage (0–100), null if not applicable */
    private Integer progressPercent;

    /** Human-readable message about the upload */
    private String message;
}
