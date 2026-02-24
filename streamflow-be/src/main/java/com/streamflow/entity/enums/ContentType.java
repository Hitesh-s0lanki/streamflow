package com.streamflow.entity.enums;

/* Enum for content type */
public enum ContentType {
    MOVIE("movie"),
    SERIES("series");

    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
