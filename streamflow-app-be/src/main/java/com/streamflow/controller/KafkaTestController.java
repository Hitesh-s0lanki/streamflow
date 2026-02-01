package com.streamflow.controller;

import com.streamflow.kafka.consumer.KafkaConsumerService;
import com.streamflow.kafka.consumer.KafkaConsumerService.ConsumedMessage;
import com.streamflow.kafka.producer.KafkaProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Test controller for Kafka producer and consumer.
 * Use these routes to verify produce/consume flow (e.g. POST send, then GET consume).
 */
@RestController
@RequestMapping("/api/kafka/test")
public class KafkaTestController {

    private final KafkaProducerService producerService;
    private final KafkaConsumerService consumerService;

    public KafkaTestController(KafkaProducerService producerService, KafkaConsumerService consumerService) {
        this.producerService = producerService;
        this.consumerService = consumerService;
    }

    /**
     * Producer test: send a message to topic_0.
     * Example: POST /api/kafka/test/send?message=hello
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(
            @RequestParam(defaultValue = "test-message") String message,
            @RequestParam(required = false) String key) {
        if (key != null && !key.isBlank()) {
            producerService.send(key, message);
        } else {
            producerService.send(message);
        }
        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "topic", KafkaProducerService.TOPIC,
                "message", message
        ));
    }

    /**
     * Consumer test: return the most recently consumed messages (in-memory buffer).
     * Example: GET /api/kafka/test/consume
     */
    @GetMapping("/consume")
    public ResponseEntity<Map<String, Object>> consume() {
        List<ConsumedMessage> recent = consumerService.getRecentMessages();
        return ResponseEntity.ok(Map.of(
                "topic", KafkaProducerService.TOPIC,
                "count", recent.size(),
                "messages", recent
        ));
    }
}
