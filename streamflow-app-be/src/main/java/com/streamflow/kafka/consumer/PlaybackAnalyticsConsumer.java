package com.streamflow.kafka.consumer;

import com.streamflow.kafka.producer.KafkaProducerService;
import com.streamflow.service.PlaybackAnalyticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes playback events from Kafka and aggregates into PlaybackAnalytics (Phase 7).
 * Input: JSON with eventType, videoAssetId, userId (nullable), currentTimeSeconds (nullable), timestamp.
 */
@Service
public class PlaybackAnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PlaybackAnalyticsConsumer.class);

    private final PlaybackAnalyticsService playbackAnalyticsService;

    public PlaybackAnalyticsConsumer(PlaybackAnalyticsService playbackAnalyticsService) {
        this.playbackAnalyticsService = playbackAnalyticsService;
    }

    @KafkaListener(
            id = "playbackAnalyticsConsumer",
            topics = KafkaProducerService.PLAYBACK_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String value = record.value();
        log.debug("Consumed playback event: key={} partition={} offset={} value={}",
                record.key(), record.partition(), record.offset(), value);
        playbackAnalyticsService.recordEventFromJson(value);
    }
}
