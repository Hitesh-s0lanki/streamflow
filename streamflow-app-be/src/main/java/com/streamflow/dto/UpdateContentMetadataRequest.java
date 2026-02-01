package com.streamflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for PATCH /api/admin/content/{contentId}. Editable metadata only;
 * contentType must not be changed.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateContentMetadataRequest {

    private String title;
    private String description;
    private Integer releaseYear;
    private String rating;
    private String posterUrl;
    private String thumbnailUrl;
}
