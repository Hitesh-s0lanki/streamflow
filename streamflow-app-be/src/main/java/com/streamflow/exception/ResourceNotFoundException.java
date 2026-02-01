package com.streamflow.exception;

import java.util.UUID;

/**
 * Thrown when a requested entity (Content, Season, Episode) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final UUID id;

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found: " + id);
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
