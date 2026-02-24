package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ContentSummaryResponse {

    private UUID id;
    private String title;
    private ContentType contentType;
    private String thumbnailUrl;
    private PublishStatus publishStatus;
    private Integer releaseYear;
    private Instant createdAt;
}
