package com.streamflow.kafka.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer service for sending messages to topics.
 */
@Service
public class KafkaProducerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducerService.class);

    public static final String TOPIC = "topic_0";

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
}
