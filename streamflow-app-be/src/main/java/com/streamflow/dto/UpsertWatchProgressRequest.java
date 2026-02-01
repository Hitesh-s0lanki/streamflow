package com.streamflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/watch-progress.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpsertWatchProgressRequest {

    @NotNull(message = "videoAssetId is required")
    private UUID videoAssetId;

    @NotNull(message = "lastWatchedSecond is required")
    @Min(value = 0, message = "lastWatchedSecond must be >= 0")
    private Integer lastWatchedSecond;

    /**
     * If provided by client, used; otherwise completion is derived from position.
     */
    private Boolean completed;
}
