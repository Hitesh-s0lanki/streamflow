package com.streamflow.service;

import com.streamflow.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes ingestion job events to Kafka so the media processing worker can
 * pick up raw uploads and run FFmpeg (transcode, sprites, etc.). Only created
 * when Kafka is configured (spring.kafka.bootstrap-servers set).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class MediaIngestionProducer {

    private final KafkaProperties kafkaProperties;
    private final KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Publish an ingestion request. Payload is JSON: jobId, videoAssetId, rawS3Key,
     * contentType, drmEnabled so the worker can process without extra DB lookups.
     */
    public CompletableFuture<SendResult<String, String>> sendIngestionRequest(UUID jobId, UUID videoAssetId,
            String rawS3Key, String contentType, boolean drmEnabled) {
        String topic = kafkaProperties.getIngestionTopic();
        String payload = buildPayload(jobId, videoAssetId, rawS3Key, contentType, drmEnabled);
        String key = jobId.toString();
        log.info("Publishing ingestion job to topic {}: jobId={}, videoAssetId={}", topic, jobId, videoAssetId);
        return kafkaTemplate.send(topic, key, payload);
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String buildPayload(UUID jobId, UUID videoAssetId, String rawS3Key, String contentType,
            boolean drmEnabled) {
        return """
                {"jobId":"%s","videoAssetId":"%s","rawS3Key":"%s","contentType":"%s","drmEnabled":%s}
                """.formatted(jobId, videoAssetId, escapeJson(rawS3Key),
                escapeJson(contentType != null ? contentType : "video/mp4"), drmEnabled);
    }
}
