package com.streamflow.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PlaybackSessionRequest {

    @NotNull(message = "content_id is required")
    private UUID contentId;
}
