package com.streamflow.worker.service;

import com.streamflow.worker.config.WorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Optional: publish completion events for UI/analytics. Only active when
 * KafkaTemplate is available.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class MediaCompletionProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkerProperties workerProperties;

    public void sendTranscodeCompleted(UUID videoAssetId, String manifestUrl) {
        send(workerProperties.getTranscodeCompletedTopic(), videoAssetId, "manifestUrl", manifestUrl);
    }

    public void sendSpritesCompleted(UUID videoAssetId) {
        send(workerProperties.getSpritesCompletedTopic(), videoAssetId, null, null);
    }

    public void sendIngestionCompleted(UUID jobId, UUID videoAssetId) {
        String payload = "{\"jobId\":\"" + jobId + "\",\"videoAssetId\":\"" + videoAssetId + "\"}";
        String topic = workerProperties.getIngestionCompletedTopic();
        if (topic != null && !topic.isBlank()) {
            kafkaTemplate.send(topic, videoAssetId.toString(), payload);
            log.debug("Published ingestion.completed: jobId={}, videoAssetId={}", jobId, videoAssetId);
        }
    }

    private void send(String topic, UUID videoAssetId, String key, String value) {
        if (topic == null || topic.isBlank())
            return;
        String payload = value != null ? "{\"videoAssetId\":\"" + videoAssetId + "\",\"" + key + "\":\"" + value + "\"}"
                : "{\"videoAssetId\":\"" + videoAssetId + "\"}";
        kafkaTemplate.send(topic, videoAssetId.toString(), payload);
        log.debug("Published to {}: videoAssetId={}", topic, videoAssetId);
    }
}
