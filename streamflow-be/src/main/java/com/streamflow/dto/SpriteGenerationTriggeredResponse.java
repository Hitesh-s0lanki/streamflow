package com.streamflow.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response when sprite sheet generation is triggered asynchronously (202
 * Accepted).
 */
@Data
@Builder
public class SpriteGenerationTriggeredResponse {

    private UUID contentId;
    private String message;
}
