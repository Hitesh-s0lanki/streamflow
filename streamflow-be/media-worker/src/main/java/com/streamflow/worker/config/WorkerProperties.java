package com.streamflow.worker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Worker-specific settings: temp dir, sprite interval, resolutions, Kafka
 * topic.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {

    /** Temp dir for download and FFmpeg output (e.g. /tmp). */
    private String tempDir = System.getProperty("java.io.tmpdir");

    /** Sprite frame interval in seconds (default 5). */
    private int spriteIntervalSeconds = 5;

    /** Thumbnail size for sprites: width. */
    private int spriteThumbWidth = 160;

    /** Thumbnail size for sprites: height. */
    private int spriteThumbHeight = 90;

    /** Kafka topic to consume (must match API's app.kafka.ingestion-topic). */
    private String ingestionTopic = "streamflow.ingestion.jobs";

    /** Optional: topic to publish transcode completed. */
    private String transcodeCompletedTopic = "media.transcode.completed";

    /** Optional: topic to publish sprites completed. */
    private String spritesCompletedTopic = "media.sprites.completed";

    /** Optional: topic to publish ingestion completed. */
    private String ingestionCompletedTopic = "media.ingestion.completed";

    /** FFmpeg process timeout in seconds (default 3600 = 1 hour). */
    private long ffmpegTimeoutSeconds = 3600;
}
