package com.streamflow.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamflow.worker.dto.IngestionEventPayload;
import com.streamflow.worker.service.IngestionPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes topic media.ingestion.requested (or app.worker.ingestion-topic).
 * Payload: jobId, videoAssetId, rawS3Key, contentType, drmEnabled. Starts the
 * processing pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MediaIngestionConsumer {

    private final IngestionPipelineService ingestionPipelineService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "${app.worker.ingestion-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void onIngestionRequested(String message) {
        log.info("Received ingestion event: {}", message);
        try {
            IngestionEventPayload payload = objectMapper.readValue(message, IngestionEventPayload.class);
            if (payload == null || payload.getJobId() == null || payload.getVideoAssetId() == null
                    || payload.getRawS3Key() == null || payload.getRawS3Key().isBlank()) {
                log.warn("Invalid payload: missing jobId, videoAssetId, or rawS3Key");
                return;
            }
            ingestionPipelineService.runPipeline(payload);
        } catch (Exception e) {
            log.error("Failed to process ingestion event: {}", message, e);
            throw e;
        }
    }
}
