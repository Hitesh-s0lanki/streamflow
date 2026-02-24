package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContentDetailResponse {

    private UUID id;
    private String title;
    private String description;
    private ContentType contentType;
    private Integer releaseYear;
    private String rating;
    private String posterUrl;
    private String thumbnailUrl;
    private PublishStatus publishStatus;
    private Integer durationSeconds;
    private Instant createdAt;
    private Instant updatedAt;
}
