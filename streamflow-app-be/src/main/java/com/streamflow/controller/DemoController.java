package com.streamflow.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Demo controller for testing purposes.
 * Exposes a version API to verify app version.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private static final String VERSION_FILE = "VERSION";

    @GetMapping("/version")
    public ResponseEntity<Map<String, String>> getVersion() {
        String version = readVersionFromClasspath();
        return ResponseEntity.ok(Map.of("version", version));
    }

    private String readVersionFromClasspath() {
        try {
            var resource = new ClassPathResource(VERSION_FILE);
            try (var inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            return "unknown";
        }
    }
}
