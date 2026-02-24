package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for creating content.
 */
@Getter
@Setter
public class CreateContentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 512, message = "Title must not exceed 512 characters")
    private String title;

    private String description;

    @NotNull(message = "Content type is required")
    private ContentType contentType;

    private Integer releaseYear;

    @Size(max = 16, message = "Rating must not exceed 16 characters")
    private String rating;

    private Integer durationSeconds;
}
