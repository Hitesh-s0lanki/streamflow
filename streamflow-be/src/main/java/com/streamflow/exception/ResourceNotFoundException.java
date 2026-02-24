package com.streamflow.exception;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final UUID id;

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(resourceType + " not found");
        log.error("Resource not found: {} {}", resourceType, id);
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
