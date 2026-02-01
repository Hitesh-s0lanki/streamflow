package com.streamflow.exception;

/**
 * Thrown when an S3 upload (image or video) fails.
 */
public class S3UploadException extends S3StorageException {

    public S3UploadException(String message, String bucket, String key) {
        super(message, bucket, key);
    }

    public S3UploadException(String message, String bucket, String key, Throwable cause) {
        super(message, bucket, key, cause);
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
