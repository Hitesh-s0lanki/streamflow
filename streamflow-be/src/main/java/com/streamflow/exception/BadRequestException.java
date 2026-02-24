package com.streamflow.exception;

/**
 * Thrown for validation/business rule violations (wrong content type,
 * duplicate season/episode number, invalid publish transition).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
