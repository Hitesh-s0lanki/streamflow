package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/content — create Movie or Series in DRAFT.
 */
@Getter
@Setter
public class CreateContentRequest {

    @NotNull(message = "title is required")
    @Size(max = 512)
    private String title;

    @NotNull(message = "contentType is required")
    private ContentType contentType;

    @Size(max = 65535)
    private String description;

    private Integer releaseYear;

    @Size(max = 16)
    private String rating;

    @Size(max = 1024)
    private String posterUrl;

    @Size(max = 1024)
    private String thumbnailUrl;
}
