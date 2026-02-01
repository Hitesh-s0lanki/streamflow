package com.streamflow.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateVideoAssetRequest {

    /**
     * Content (Movie or Series) this asset belongs to. For MOVIE, the asset is
     * linked as content.videoAsset. For SERIES, use episode creation (which
     * creates the asset) or attach to an existing episode later.
     */
    @NotNull(message = "contentId is required")
    private UUID contentId;

    /**
     * Duration in seconds; use 0 as placeholder until upload/processing completes.
     */
    private Integer durationSeconds = 0;
}
