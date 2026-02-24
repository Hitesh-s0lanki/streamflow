package com.streamflow.service;

import com.streamflow.dto.PlaybackSessionResponse;
import com.streamflow.dto.PlaybackSessionResponse.ContentInfo;
import com.streamflow.dto.PlaybackSessionResponse.SpriteInfo;
import com.streamflow.dto.PlaybackSessionResponse.SpriteSheetInfo;
import com.streamflow.dto.PlaybackSessionResponse.StreamInfo;
import com.streamflow.entity.Content;
import com.streamflow.entity.SpriteSheet;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.ProcessingStatus;
import com.streamflow.entity.enums.PublishStatus;
import com.streamflow.entity.enums.UploadStatus;
import com.streamflow.exception.BadRequestException;
import com.streamflow.exception.ResourceNotFoundException;
import com.streamflow.repository.ContentRepository;
import com.streamflow.repository.SpriteSheetRepository;
import com.streamflow.service.S3StorageService.PresignedGetUrlResult;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Handles playback session creation for HLS streaming.
 * Validates content readiness, generates pre-signed S3 URLs for
 * the HLS manifest and sprite sheets, and returns everything the
 * player needs in a single response — no video bytes ever pass
 * through this service.
 */
@Service
@RequiredArgsConstructor
public class PlaybackService {

    private final ContentRepository contentRepository;
    private final SpriteSheetRepository spriteSheetRepository;
    private final S3StorageService s3StorageService;

    @Value("${streamflow.playback.manifest-url-expiry-minutes:30}")
    private int manifestUrlExpiryMinutes;

    @Value("${streamflow.playback.sprite-url-expiry-minutes:60}")
    private int spriteUrlExpiryMinutes;

    @Value("${streamflow.playback.asset-url-expiry-minutes:60}")
    private int assetUrlExpiryMinutes;

    /** When set, playback manifest URL is the stream proxy (fixes 403 on variant/segment requests). */
    @Value("${streamflow.api.base-url:}")
    private String apiBaseUrl;

    /**
     * Creates a playback session for the given content.
     *
     * <ol>
     *   <li>Fetches Content; validates publish_status = PUBLISHED</li>
     *   <li>Fetches VideoAsset; validates upload + processing complete</li>
     *   <li>Generates a pre-signed GET URL for the HLS master manifest</li>
     *   <li>Generates pre-signed GET URLs for every sprite sheet</li>
     *   <li>Optionally resolves poster/thumbnail to pre-signed URLs</li>
     * </ol>
     *
     * @param contentId UUID of the content to play
     * @return PlaybackSessionResponse ready for the frontend player
     */
    @Transactional(readOnly = true)
    public PlaybackSessionResponse createSession(UUID contentId) {
        Content content = contentRepository.findByIdWithVideoAsset(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));

        validatePublished(content);

        VideoAsset videoAsset = content.getVideoAsset();
        if (videoAsset == null) {
            throw new ResourceNotFoundException("VideoAsset for Content", contentId);
        }

        validateStreamReady(videoAsset, contentId);

        List<SpriteSheet> sheets = spriteSheetRepository
                .findAllByVideoAssetIdOrderBySheetIndexAsc(videoAsset.getId());

        // Generate all presigned URLs in parallel (manifest, poster, thumbnail, N sprites)
        CompletableFuture<PresignedGetUrlResult> manifestFuture = CompletableFuture.supplyAsync(() ->
                s3StorageService.generatePresignedGetUrl(
                        videoAsset.getManifestUrl(), manifestUrlExpiryMinutes));
        CompletableFuture<String> posterFuture = CompletableFuture
                .supplyAsync(() -> resolveAssetUrl(content.getPosterUrl()));
        CompletableFuture<String> thumbnailFuture = CompletableFuture
                .supplyAsync(() -> resolveAssetUrl(content.getThumbnailUrl()));
        List<CompletableFuture<SpriteSheetInfo>> spriteFutures = sheets.stream()
                .map(sheet -> CompletableFuture.supplyAsync(() -> toSpriteSheetInfo(sheet)))
                .toList();

        CompletableFuture<?>[] all = new CompletableFuture[3 + spriteFutures.size()];
        all[0] = manifestFuture;
        all[1] = posterFuture;
        all[2] = thumbnailFuture;
        for (int i = 0; i < spriteFutures.size(); i++) {
            all[3 + i] = spriteFutures.get(i);
        }
        CompletableFuture.allOf(all).join();

        PresignedGetUrlResult manifestResult = manifestFuture.join();
        String posterUrl = posterFuture.join();
        String thumbnailUrl = thumbnailFuture.join();
        List<SpriteSheetInfo> sheetInfos = spriteFutures.stream()
                .map(CompletableFuture::join)
                .toList();

        ContentInfo contentInfo = buildContentInfo(content, videoAsset, posterUrl, thumbnailUrl);
        StreamInfo streamInfo = buildStreamInfo(manifestResult, videoAsset, content.getId());
        SpriteInfo spriteInfo = buildSpriteInfoFromSheets(sheets, sheetInfos);

        return PlaybackSessionResponse.builder()
                .sessionId(UUID.randomUUID())
                .content(contentInfo)
                .stream(streamInfo)
                .sprites(spriteInfo)
                .expiresAt(manifestResult.getExpiresAt())
                .build();
    }

    private void validatePublished(Content content) {
        if (content.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new BadRequestException(
                    "Content is not published (current status: " + content.getPublishStatus() + ")");
        }
    }

    private void validateStreamReady(VideoAsset videoAsset, UUID contentId) {
        if (videoAsset.getUploadStatus() != UploadStatus.COMPLETED) {
            throw new BadRequestException(
                    "Video upload is not complete for content " + contentId
                            + " (current status: " + videoAsset.getUploadStatus() + ")");
        }
        if (videoAsset.getProcessingStatus() != ProcessingStatus.COMPLETED) {
            throw new BadRequestException(
                    "Video processing is not complete for content " + contentId
                            + " (current status: " + videoAsset.getProcessingStatus() + ")");
        }
        if (videoAsset.getManifestUrl() == null || videoAsset.getManifestUrl().isBlank()) {
            throw new BadRequestException(
                    "HLS manifest not available for content " + contentId);
        }
    }

    /**
     * Returns the S3 key prefix (directory) for HLS objects for the given content.
     * Used by the stream proxy so variant playlists and segments can be resolved.
     * Validates content is published and stream is ready.
     *
     * @param contentId content UUID
     * @return trailing-slash prefix, e.g. "hls/28ea3c72-.../"
     */
    public String getManifestKeyPrefix(UUID contentId) {
        Content content = contentRepository.findByIdWithVideoAsset(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
        validatePublished(content);
        VideoAsset videoAsset = content.getVideoAsset();
        if (videoAsset == null) {
            throw new ResourceNotFoundException("VideoAsset for Content", contentId);
        }
        validateStreamReady(videoAsset, contentId);
        String manifestUrl = videoAsset.getManifestUrl();
        int lastSlash = manifestUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            return manifestUrl + "/";
        }
        return manifestUrl.substring(0, lastSlash + 1);
    }

    private ContentInfo buildContentInfo(Content content, VideoAsset videoAsset,
            String posterUrl, String thumbnailUrl) {
        Integer duration = videoAsset.getDurationSeconds() != null && videoAsset.getDurationSeconds() > 0
                ? videoAsset.getDurationSeconds()
                : content.getDurationSeconds();

        return ContentInfo.builder()
                .id(content.getId())
                .title(content.getTitle())
                .durationSeconds(duration)
                .posterUrl(posterUrl)
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private StreamInfo buildStreamInfo(PresignedGetUrlResult manifestResult, VideoAsset videoAsset, UUID contentId) {
        String manifestUrl;
        if (StringUtils.hasText(apiBaseUrl)) {
            String base = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
            manifestUrl = base + "/api/media/playback/stream/" + contentId + "/master.m3u8";
        } else {
            manifestUrl = manifestResult.getUrl();
        }
        return StreamInfo.builder()
                .type("HLS")
                .manifestUrl(manifestUrl)
                .drmEnabled(videoAsset.getDrmEnabled())
                .build();
    }

    private SpriteInfo buildSpriteInfoFromSheets(List<SpriteSheet> sheets, List<SpriteSheetInfo> sheetInfos) {
        if (sheets.isEmpty()) {
            return SpriteInfo.builder()
                    .sheets(Collections.emptyList())
                    .build();
        }
        SpriteSheet first = sheets.getFirst();
        return SpriteInfo.builder()
                .intervalSeconds(first.getIntervalSeconds())
                .thumbWidth(first.getThumbWidth())
                .thumbHeight(first.getThumbHeight())
                .sheets(sheetInfos)
                .build();
    }

    private SpriteSheetInfo toSpriteSheetInfo(SpriteSheet sheet) {
        PresignedGetUrlResult spriteResult = s3StorageService.generatePresignedGetUrl(
                sheet.getSpriteS3Key(), spriteUrlExpiryMinutes);

        int startTimeSeconds = sheet.getStartFrame() * sheet.getIntervalSeconds();
        int endTimeSeconds = sheet.getEndFrame() * sheet.getIntervalSeconds();

        return SpriteSheetInfo.builder()
                .sheetIndex(sheet.getSheetIndex())
                .startTimeSeconds(startTimeSeconds)
                .endTimeSeconds(endTimeSeconds)
                .rowsCount(sheet.getRowsCount())
                .columnsCount(sheet.getColumnsCount())
                .framesCount(sheet.getFramesCount())
                .spriteUrl(spriteResult.getUrl())
                .build();
    }

    /**
     * Resolves an S3 key (poster/thumbnail) to a pre-signed GET URL.
     * Returns null if the key is null or blank.
     */
    private String resolveAssetUrl(String s3Key) {
        if (s3Key == null || s3Key.isBlank()) {
            return null;
        }
        return s3StorageService.generatePresignedGetUrl(s3Key, assetUrlExpiryMinutes).getUrl();
    }
}
