package com.streamflow.config;

import com.streamflow.dto.ConfirmUploadRequest;
import com.streamflow.dto.CreateContentRequest;
import com.streamflow.dto.CreateVideoAssetRequest;
import com.streamflow.entity.Content;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.ContentType;
import com.streamflow.entity.enums.PublishStatus;

import java.util.UUID;

/**
 * Shared test data: fixed UUIDs and factory methods for requests/entities.
 * Keeps service unit tests clean and consistent.
 */
public final class TestFixtures {

    private TestFixtures() {}

    public static final UUID CONTENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID VIDEO_ASSET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID EPISODE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID SEASON_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID INGESTION_JOB_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final UUID LICENSE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    public static final String USER_ID = "user-test-123";

    public static CreateContentRequest createContentRequest(String title, ContentType type) {
        CreateContentRequest r = new CreateContentRequest();
        r.setTitle(title);
        r.setContentType(type);
        r.setDescription("Description");
        r.setReleaseYear(2024);
        return r;
    }

    public static CreateVideoAssetRequest createVideoAssetRequest(UUID contentId, UUID episodeId, int durationSeconds) {
        CreateVideoAssetRequest r = new CreateVideoAssetRequest();
        r.setContentId(contentId);
        r.setEpisodeId(episodeId);
        r.setDurationSeconds(durationSeconds);
        return r;
    }

    public static ConfirmUploadRequest confirmUploadRequest(String rawS3Key, String contentType) {
        ConfirmUploadRequest r = new ConfirmUploadRequest();
        r.setRawS3Key(rawS3Key);
        r.setContentType(contentType != null ? contentType : "video/mp4");
        return r;
    }

    public static Content content(UUID id, String title, ContentType type, PublishStatus status) {
        Content c = new Content();
        c.setId(id);
        c.setTitle(title);
        c.setContentType(type);
        c.setPublishStatus(status);
        c.setReleaseYear(2024);
        return c;
    }

    public static VideoAsset videoAsset(UUID id, Content content, int durationSeconds) {
        VideoAsset v = new VideoAsset();
        v.setId(id);
        v.setContent(content);
        v.setEpisode(null);
        v.setDurationSeconds(durationSeconds);
        v.setDrmEnabled(false);
        return v;
    }
}
