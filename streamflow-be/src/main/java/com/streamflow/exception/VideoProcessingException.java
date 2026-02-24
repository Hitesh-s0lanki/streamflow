package com.streamflow.exception;

/**
 * Exception thrown when video processing (e.g. ffprobe/ffmpeg) fails.
 */
public class VideoProcessingException extends RuntimeException {

    public VideoProcessingException(String message) {
        super(message);
    }

    public VideoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
