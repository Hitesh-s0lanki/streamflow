package com.streamflow.dto.admin;

import com.streamflow.entity.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateContentRequest {

    @NotBlank(message = "title is required")
    private String title;

    private String description;

    @NotNull(message = "contentType is required")
    private ContentType contentType;

    private Integer releaseYear;
    private String rating;
    private String posterUrl;
    private String thumbnailUrl;

    /** Duration in seconds; used for MOVIE. */
    private Integer durationSeconds;
}
