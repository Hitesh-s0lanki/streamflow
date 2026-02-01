package com.streamflow.kafka.consumer;

import com.streamflow.kafka.producer.KafkaProducerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kafka consumer service for receiving messages from topics.
 * Keeps the last N consumed messages in memory for test route inspection.
 */
@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    private static final int MAX_RECENT_MESSAGES = 100;

    private final List<ConsumedMessage> recentMessages = new CopyOnWriteArrayList<>();

    @KafkaListener(
            id = "streamflowConsumer",
            topics = KafkaProducerService.TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String key = record.key();
        String value = record.value();
        log.info("Consumed event: key={} value={} partition={} offset={}", key, value, record.partition(), record.offset());
        addRecent(value, key);
    }

    private void addRecent(String value, String key) {
        recentMessages.add(new ConsumedMessage(key, value, System.currentTimeMillis()));
        while (recentMessages.size() > MAX_RECENT_MESSAGES) {
            recentMessages.remove(0);
        }
    }

    /**
     * Returns the most recently consumed messages (for test route).
     */
    public List<ConsumedMessage> getRecentMessages() {
        List<ConsumedMessage> copy = new ArrayList<>(recentMessages);
        Collections.reverse(copy);
        return copy;
    }

    public record ConsumedMessage(String key, String value, long timestamp) {}
}
