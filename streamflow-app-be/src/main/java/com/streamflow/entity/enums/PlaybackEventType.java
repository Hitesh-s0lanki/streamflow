package com.streamflow.entity.enums;

/**
 * Types of playback events for analytics (Kafka / event-driven).
 */
public enum PlaybackEventType {
    PLAY,
    PAUSE,
    SEEK,
    BUFFERING,
    COMPLETED,
    ABANDONED,
    ERROR
}
