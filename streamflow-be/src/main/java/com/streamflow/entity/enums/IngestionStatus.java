package com.streamflow.entity.enums;

/**
 * Lifecycle state of an ingestion job (upload → processing → ready).
 */
public enum IngestionStatus {
    PENDING,
    UPLOADING,
    UPLOADED,
    PROCESSING,
    TRANSCODED,
    SPRITES_GENERATED,
    READY,
    FAILED
}
