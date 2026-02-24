package com.streamflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PlaybackSessionResponse {

    @JsonProperty("session_id")
    private UUID sessionId;

    private ContentInfo content;

    private StreamInfo stream;

    private SpriteInfo sprites;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    @Data
    @Builder
    public static class ContentInfo {
        private UUID id;
        private String title;

        @JsonProperty("duration_seconds")
        private Integer durationSeconds;

        @JsonProperty("poster_url")
        private String posterUrl;

        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;
    }

    @Data
    @Builder
    public static class StreamInfo {
        private String type;

        @JsonProperty("manifest_url")
        private String manifestUrl;

        @JsonProperty("drm_enabled")
        private Boolean drmEnabled;
    }

    @Data
    @Builder
    public static class SpriteInfo {

        @JsonProperty("interval_seconds")
        private Integer intervalSeconds;

        @JsonProperty("thumb_width")
        private Integer thumbWidth;

        @JsonProperty("thumb_height")
        private Integer thumbHeight;

        private List<SpriteSheetInfo> sheets;
    }

    @Data
    @Builder
    public static class SpriteSheetInfo {

        @JsonProperty("sheet_index")
        private Integer sheetIndex;

        @JsonProperty("start_time_seconds")
        private Integer startTimeSeconds;

        @JsonProperty("end_time_seconds")
        private Integer endTimeSeconds;

        @JsonProperty("rows_count")
        private Integer rowsCount;

        @JsonProperty("columns_count")
        private Integer columnsCount;

        @JsonProperty("frames_count")
        private Integer framesCount;

        @JsonProperty("sprite_url")
        private String spriteUrl;
    }
}
