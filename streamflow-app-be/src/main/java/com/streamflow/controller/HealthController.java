package com.streamflow.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operational health and readiness. No secrets or raw URLs in responses.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final String kafkaBootstrapServers;
    private final boolean s3Enabled;
    private final String s3Bucket;

    public HealthController(JdbcTemplate jdbcTemplate,
            @Value("${spring.kafka.bootstrap-servers:}") String kafkaBootstrapServers,
            @Value("${streamflow.aws.s3.enabled:false}") boolean s3Enabled,
            @Value("${streamflow.aws.s3.bucket:}") String s3Bucket) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaBootstrapServers = kafkaBootstrapServers != null ? kafkaBootstrapServers : "";
        this.s3Enabled = s3Enabled;
        this.s3Bucket = s3Bucket != null ? s3Bucket : "";
    }

    /**
     * Liveness: service is up.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /**
     * Readiness: database, Kafka config, S3 config (and optional connectivity).
     */
    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> checks = new LinkedHashMap<>();
        boolean allUp = true;

        // Database connectivity
        boolean dbUp = checkDatabase();
        checks.put("database", dbUp ? "up" : "down");
        if (!dbUp) allUp = false;

        // Kafka: config present (and optionally connectivity - we only check config for simplicity)
        boolean kafkaConfigPresent = kafkaBootstrapServers != null && !kafkaBootstrapServers.isBlank();
        checks.put("kafka", kafkaConfigPresent ? "up" : "down");
        if (!kafkaConfigPresent) allUp = false;

        // S3: if enabled, config must be present
        if (s3Enabled) {
            boolean s3ConfigPresent = s3Bucket != null && !s3Bucket.isBlank();
            checks.put("s3", s3ConfigPresent ? "up" : "down");
            if (!s3ConfigPresent) allUp = false;
        } else {
            checks.put("s3", "not_configured");
        }

        checks.put("status", allUp ? "UP" : "DOWN");
        return allUp ? ResponseEntity.ok(checks) : ResponseEntity.status(503).body(checks);
    }

    private boolean checkDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
