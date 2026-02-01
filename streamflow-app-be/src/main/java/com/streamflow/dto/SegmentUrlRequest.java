package com.streamflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request body for POST /api/playback/{videoAssetId}/segment-url.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentUrlRequest {

    @NotNull(message = "licenseId is required")
    private UUID licenseId;

    /**
     * S3 key or path of the segment; must belong to a VideoVariant of this asset.
     */
    @NotBlank(message = "segmentPath is required")
    private String segmentPath;
}
