package com.streamflow.exception;

/**
 * Base exception for S3 storage operations (upload, delete, etc.).
 */
public class S3StorageException extends RuntimeException {

    private final String bucket;
    private final String key;

    public S3StorageException(String message, String bucket, String key) {
        super(message);
        this.bucket = bucket;
        this.key = key;
    }

    public S3StorageException(String message, String bucket, String key, Throwable cause) {
        super(message, cause);
        this.bucket = bucket;
        this.key = key;
    }

    public S3StorageException(String message, Throwable cause) {
        super(message, cause);
        this.bucket = null;
        this.key = null;
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }
}
