package com.streamflow.controller;

import com.streamflow.dto.HealthResponse;
import com.streamflow.repository.ContentRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";

    private final ContentRepository contentRepository;

    public HealthController(ContentRepository contentRepository) {
        this.contentRepository = contentRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        String version = readVersion();
        Map<String, String> components = new HashMap<>();
        components.put("database", checkDatabase());

        boolean allUp = components.values().stream().allMatch(UP::equals);
        String status = allUp ? UP : DOWN;

        HealthResponse body = HealthResponse.builder()
                .status(status)
                .version(version)
                .timestamp(Instant.now())
                .components(components)
                .build();

        return allUp ? ResponseEntity.ok(body) : ResponseEntity.status(503).body(body);
    }

    private String readVersion() {
        try {
            var resource = new ClassPathResource("VERSION");
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String checkDatabase() {
        try {
            contentRepository.count();
            return UP;
        } catch (Exception e) {
            return DOWN;
        }
    }
}
