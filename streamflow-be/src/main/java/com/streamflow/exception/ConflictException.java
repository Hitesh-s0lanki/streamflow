package com.streamflow.exception;

import java.util.UUID;

/**
 * Thrown for duplicate or conflicting state (e.g. duplicate seasonNumber,
 * episodeNumber, videoAsset; invalid state transition).
 */
public class ConflictException extends RuntimeException {

    private final String resourceType;
    private final UUID id;

    public ConflictException(String message) {
        super(message);
        this.resourceType = null;
        this.id = null;
    }

    public ConflictException(String message, String resourceType, UUID id) {
        super(message);
        this.resourceType = resourceType;
        this.id = id;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getId() {
        return id;
    }
}
