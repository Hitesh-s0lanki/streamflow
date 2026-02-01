package com.streamflow.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka producer service for sending messages to topics.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    public static final String TOPIC = "topic_0";
    /**
     * Topic for ingestion pipeline: video upload complete → transcoding/sprite
     * generation.
     */
    public static final String INGESTION_TOPIC = "streamflow-ingestion";
    /**
     * Topic for playback events (Phase 6): PLAY, PAUSE, SEEK, BUFFERING, COMPLETED,
     * etc.
     */
    public static final String PLAYBACK_EVENTS_TOPIC = "streamflow-playback-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message to the default test topic.
     *
     * @param key   optional key (can be null)
     * @param value message value
     */
    public void send(String key, String value) {
        kafkaTemplate.send(TOPIC, key, value)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send message to topic {}: {}", TOPIC, ex.getMessage());
                    } else {
                        log.info("Produced event to topic {}: key={} value={} partition={} offset={}",
                                TOPIC, key, value,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Sends a message with no key (null key).
     */
    public void send(String value) {
        send(null, value);
    }

    /**
     * Emits an ingestion event after upload is confirmed. Payload: videoAssetId,
     * rawS3Key,
     * contentType, episodeId (if series), movieId (if movie).
     */
    public void sendIngestionEvent(UUID videoAssetId, String rawS3Key, String contentType,
            UUID movieId, UUID episodeId) {
        String ct = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
        StringBuilder sb = new StringBuilder()
                .append("{\"videoAssetId\":\"").append(videoAssetId).append("\",\"rawS3Key\":\"")
                .append(escapeJson(rawS3Key)).append("\",\"contentType\":\"").append(escapeJson(ct)).append("\"");
        if (movieId != null) {
            sb.append(",\"movieId\":\"").append(movieId).append("\"");
        }
        if (episodeId != null) {
            sb.append(",\"episodeId\":\"").append(episodeId).append("\"");
        }
        sb.append("}");
        String value = sb.toString();
        String key = videoAssetId.toString();
        kafkaTemplate.send(INGESTION_TOPIC, key, value)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send ingestion event to topic {}: {}", INGESTION_TOPIC, ex.getMessage());
                    } else {
                        log.info("Produced ingestion event: videoAssetId={} partition={} offset={}",
                                videoAssetId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * Emits a playback event to the analytics topic (Phase 6). Payload: eventType,
     * videoAssetId, userId (nullable),
     * currentTimeSeconds (nullable), timestamp.
     */
    public void sendPlaybackEvent(String eventType, UUID videoAssetId, String userId, Integer currentTimeSeconds,
            Instant timestamp) {
        StringBuilder sb = new StringBuilder()
                .append("{\"eventType\":\"").append(escapeJson(eventType)).append("\",\"videoAssetId\":\"")
                .append(videoAssetId).append("\"");
        sb.append(",\"userId\":");
        if (userId == null || userId.isBlank()) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(userId)).append("\"");
        }
        sb.append(",\"currentTimeSeconds\":");
        sb.append(currentTimeSeconds != null ? currentTimeSeconds : "null");
        sb.append(",\"timestamp\":\"")
                .append(timestamp != null ? timestamp.toString() : Instant.now().toString()).append("\"}");
        String value = sb.toString();
        String key = videoAssetId.toString();
        kafkaTemplate.send(PLAYBACK_EVENTS_TOPIC, key, value)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send playback event to topic {}: {}", PLAYBACK_EVENTS_TOPIC,
                                ex.getMessage());
                    } else {
                        log.debug("Produced playback event: eventType={} videoAssetId={} partition={} offset={}",
                                eventType, videoAssetId,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
