package com.streamflow.entity.enums;

/**
 * Enum representing the status of a video upload process.
 */
public enum UploadStatus {
    
    /** Upload has been initiated but not started */
    PENDING("pending"),
    
    /** Multipart upload has been initiated, waiting for parts */
    MULTIPART_INITIATED("multipart_initiated"),
    
    /** Upload is currently in progress */
    UPLOADING("uploading"),
    
    /** Upload completed successfully */
    COMPLETED("completed"),
    
    /** Upload failed due to an error */
    FAILED("failed"),
    
    /** Upload was cancelled/aborted */
    CANCELLED("cancelled");

    private final String value;

    UploadStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
