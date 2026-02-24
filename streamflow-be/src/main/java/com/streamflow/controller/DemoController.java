package com.streamflow.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @GetMapping("/version")
    public ResponseEntity<String> getVersion() throws IOException {
        var resource = new ClassPathResource("VERSION");
        String version = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        return ResponseEntity.ok(version);
    }

}
