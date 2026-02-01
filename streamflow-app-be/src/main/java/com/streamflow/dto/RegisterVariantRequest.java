package com.streamflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/video-assets/{videoAssetId}/variants.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVariantRequest {

    @NotBlank(message = "resolution is required")
    private String resolution;

    private Integer bitrateKbps;

    private String codec;

    private String segmentPath;

    @NotNull(message = "sortOrder is required")
    private Integer sortOrder;
}
