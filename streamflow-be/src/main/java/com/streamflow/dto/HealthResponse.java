package com.streamflow.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class HealthResponse {

    private String status;
    private String version;
    private Instant timestamp;
    private Map<String, String> components;
}
