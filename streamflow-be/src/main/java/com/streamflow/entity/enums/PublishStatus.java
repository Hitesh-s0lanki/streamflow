package com.streamflow.entity.enums;

/* Enum for publish status */
public enum PublishStatus {
    DRAFT("draft"),
    PUBLISHED("published");

    private final String value;

    PublishStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
