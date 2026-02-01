package com.streamflow.dto;

import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin content list item (includes DRAFT and publishStatus).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContentListItemResponse {

    private UUID id;
    private String title;
    private ContentType contentType;
    private PublishStatus publishStatus;
    private String posterUrl;
    private String thumbnailUrl;
    private Integer releaseYear;
    private Integer durationSeconds;
    private Instant createdAt;
}
