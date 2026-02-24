package com.streamflow.exception;

import java.util.UUID;

/**
 * Exception thrown when video upload operations fail.
 * Contains contextual information about the upload attempt for debugging and error handling.
 */
public class VideoUploadException extends RuntimeException {

    private final UUID contentId;
    private final UUID videoAssetId;
    private final String uploadId;
    private final UploadPhase phase;

    /**
     * Enum representing the phase of upload where the error occurred.
     */
    public enum UploadPhase {
        VALIDATION,
        INITIALIZATION,
        MULTIPART_INITIATION,
        PART_UPLOAD,
        COMPLETION,
        CLEANUP,
        ABORT
    }

    public VideoUploadException(String message, UUID contentId) {
        super(message);
        this.contentId = contentId;
        this.videoAssetId = null;
        this.uploadId = null;
        this.phase = UploadPhase.VALIDATION;
    }

    public VideoUploadException(String message, UUID contentId, UploadPhase phase) {
        super(message);
        this.contentId = contentId;
        this.videoAssetId = null;
        this.uploadId = null;
        this.phase = phase;
    }

    public VideoUploadException(String message, UUID contentId, UUID videoAssetId, UploadPhase phase) {
        super(message);
        this.contentId = contentId;
        this.videoAssetId = videoAssetId;
        this.uploadId = null;
        this.phase = phase;
    }

    public VideoUploadException(String message, UUID contentId, UUID videoAssetId, String uploadId, UploadPhase phase) {
        super(message);
        this.contentId = contentId;
        this.videoAssetId = videoAssetId;
        this.uploadId = uploadId;
        this.phase = phase;
    }

    public VideoUploadException(String message, UUID contentId, Throwable cause) {
        super(message, cause);
        this.contentId = contentId;
        this.videoAssetId = null;
        this.uploadId = null;
        this.phase = UploadPhase.VALIDATION;
    }

    public VideoUploadException(String message, UUID contentId, UploadPhase phase, Throwable cause) {
        super(message, cause);
        this.contentId = contentId;
        this.videoAssetId = null;
        this.uploadId = null;
        this.phase = phase;
    }

    public VideoUploadException(String message, UUID contentId, UUID videoAssetId, UploadPhase phase, Throwable cause) {
        super(message, cause);
        this.contentId = contentId;
        this.videoAssetId = videoAssetId;
        this.uploadId = null;
        this.phase = phase;
    }

    public VideoUploadException(String message, UUID contentId, UUID videoAssetId, String uploadId, 
                                 UploadPhase phase, Throwable cause) {
        super(message, cause);
        this.contentId = contentId;
        this.videoAssetId = videoAssetId;
        this.uploadId = uploadId;
        this.phase = phase;
    }

    public UUID getContentId() {
        return contentId;
    }

    public UUID getVideoAssetId() {
        return videoAssetId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public UploadPhase getPhase() {
        return phase;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VideoUploadException{");
        sb.append("message='").append(getMessage()).append('\'');
        if (contentId != null) {
            sb.append(", contentId=").append(contentId);
        }
        if (videoAssetId != null) {
            sb.append(", videoAssetId=").append(videoAssetId);
        }
        if (uploadId != null) {
            sb.append(", uploadId='").append(uploadId).append('\'');
        }
        if (phase != null) {
            sb.append(", phase=").append(phase);
        }
        sb.append('}');
        return sb.toString();
    }
}
