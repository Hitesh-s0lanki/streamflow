package com.streamflow.entity.enums;

public enum ProcessingStatus {

    NONE("none"),
    QUEUED("queued"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    ProcessingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
