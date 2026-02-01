package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal fields for GET /api/content (home screen tiles).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentCatalogItemResponse {

    private UUID id;
    private String title;
    private ContentType contentType;
    private String posterUrl;
    private String thumbnailUrl;
    private Integer releaseYear;
    private Integer durationSeconds;
}
