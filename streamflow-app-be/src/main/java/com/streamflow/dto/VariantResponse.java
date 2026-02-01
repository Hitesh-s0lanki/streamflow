package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Response for a registered video variant (ABR rendition).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantResponse {

    private UUID id;
    private UUID videoAssetId;
    private String resolution;
    private Integer bitrateKbps;
    private String codec;
    private String segmentPath;
    private Integer sortOrder;
}
