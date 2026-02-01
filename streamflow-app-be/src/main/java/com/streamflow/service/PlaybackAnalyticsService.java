package com.streamflow.service;

import com.streamflow.entity.PlaybackAnalytics;
import com.streamflow.entity.PlaybackWindowViewer;
import com.streamflow.entity.VideoAsset;
import com.streamflow.entity.enums.PlaybackEventType;
import com.streamflow.repository.PlaybackAnalyticsRepository;
import com.streamflow.repository.PlaybackWindowViewerRepository;
import com.streamflow.repository.VideoAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregates playback events into PlaybackAnalytics (time-window based).
 * Idempotent per window; used by Kafka consumer and optional admin rebuild.
 */
@Service
public class PlaybackAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PlaybackAnalyticsService.class);
    private static final BigDecimal ONE = new BigDecimal("1.0000");
    private static final int RATE_SCALE = 4;

    private final PlaybackAnalyticsRepository analyticsRepository;
    private final PlaybackWindowViewerRepository windowViewerRepository;
    private final VideoAssetRepository videoAssetRepository;

    public PlaybackAnalyticsService(PlaybackAnalyticsRepository analyticsRepository,
            PlaybackWindowViewerRepository windowViewerRepository,
            VideoAssetRepository videoAssetRepository) {
        this.analyticsRepository = analyticsRepository;
        this.windowViewerRepository = windowViewerRepository;
        this.videoAssetRepository = videoAssetRepository;
    }

    /**
     * Process one playback event and upsert analytics for its time window (hourly).
     * Idempotent: replaying the same event does not double-count (uniqueViewers via
     * PlaybackWindowViewer).
     */
    @Transactional
    public void recordEvent(UUID videoAssetId, PlaybackEventType eventType, String userId,
            Integer currentTimeSeconds, Instant timestamp) {
        if (videoAssetId == null || eventType == null || timestamp == null) {
            log.warn("Skipping event with null videoAssetId, eventType or timestamp");
            return;
        }
        VideoAsset videoAsset = videoAssetRepository.findById(videoAssetId).orElse(null);
        if (videoAsset == null) {
            log.warn("Skipping playback event for unknown videoAssetId: {}", videoAssetId);
            return;
        }
        Instant periodStart = timestamp.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS).toInstant();
        Instant periodEnd = periodStart.plus(1, ChronoUnit.HOURS);

        PlaybackAnalytics analytics = analyticsRepository.findByVideoAssetIdAndPeriodStart(videoAssetId, periodStart)
                .orElseGet(() -> {
                    PlaybackAnalytics a = new PlaybackAnalytics();
                    a.setVideoAsset(videoAsset);
                    a.setPeriodStart(periodStart);
                    a.setPeriodEnd(periodEnd);
                    a.setTotalPlays(0L);
                    a.setUniqueViewers(0L);
                    a.setAvgWatchTimeSeconds(null);
                    a.setCompletionRate(null);
                    a.setBufferingRate(null);
                    a.setTotalCompleted(0L);
                    a.setTotalBuffering(0L);
                    a.setTotalWatchTimeSeconds(0L);
                    return analyticsRepository.save(a);
                });

        boolean changed = false;
        if (eventType == PlaybackEventType.PLAY) {
            analytics.setTotalPlays(Optional.ofNullable(analytics.getTotalPlays()).orElse(0L) + 1);
            if (userId != null && !userId.isBlank()) {
                if (windowViewerRepository.findByVideoAssetIdAndPeriodStartAndUserId(videoAssetId, periodStart, userId)
                        .isEmpty()) {
                    PlaybackWindowViewer wv = new PlaybackWindowViewer();
                    wv.setVideoAsset(videoAsset);
                    wv.setPeriodStart(periodStart);
                    wv.setUserId(userId);
                    windowViewerRepository.save(wv);
                }
                long unique = windowViewerRepository.countByVideoAssetIdAndPeriodStart(videoAssetId, periodStart);
                analytics.setUniqueViewers(unique);
            }
            changed = true;
        } else if (eventType == PlaybackEventType.COMPLETED) {
            long totalCompleted = Optional.ofNullable(analytics.getTotalCompleted()).orElse(0L) + 1;
            analytics.setTotalCompleted(totalCompleted);
            long totalWatch = Optional.ofNullable(analytics.getTotalWatchTimeSeconds()).orElse(0L)
                    + (currentTimeSeconds != null && currentTimeSeconds >= 0 ? currentTimeSeconds : 0);
            analytics.setTotalWatchTimeSeconds(totalWatch);
            if (totalCompleted > 0) {
                analytics.setAvgWatchTimeSeconds((int) (totalWatch / totalCompleted));
            }
            changed = true;
        } else if (eventType == PlaybackEventType.BUFFERING) {
            analytics.setTotalBuffering(Optional.ofNullable(analytics.getTotalBuffering()).orElse(0L) + 1);
            changed = true;
        }

        if (changed) {
            long plays = Optional.ofNullable(analytics.getTotalPlays()).orElse(0L);
            if (plays > 0) {
                long completed = Optional.ofNullable(analytics.getTotalCompleted()).orElse(0L);
                analytics.setCompletionRate(capRate(BigDecimal.valueOf(completed).divide(BigDecimal.valueOf(plays),
                        RATE_SCALE, RoundingMode.HALF_UP)));
                long buffering = Optional.ofNullable(analytics.getTotalBuffering()).orElse(0L);
                analytics.setBufferingRate(capRate(BigDecimal.valueOf(buffering).divide(BigDecimal.valueOf(plays),
                        RATE_SCALE, RoundingMode.HALF_UP)));
            }
            analyticsRepository.save(analytics);
        }
    }

    private static BigDecimal capRate(BigDecimal rate) {
        if (rate == null)
            return null;
        return rate.compareTo(ONE) > 0 ? ONE : rate;
    }

    /**
     * Parse JSON payload and call recordEvent. Expects: eventType, videoAssetId,
     * userId (nullable), currentTimeSeconds (nullable), timestamp.
     */
    public void recordEventFromJson(String json) {
        try {
            String eventTypeStr = extractJsonString(json, "eventType");
            String videoAssetIdStr = extractJsonString(json, "videoAssetId");
            String userId = extractJsonStringOrNull(json, "userId");
            Integer currentTimeSeconds = extractJsonIntOrNull(json, "currentTimeSeconds");
            String tsStr = extractJsonString(json, "timestamp");
            Instant timestamp = tsStr != null && !tsStr.isEmpty() ? Instant.parse(tsStr) : Instant.now();

            if (eventTypeStr == null || eventTypeStr.isEmpty() || videoAssetIdStr == null
                    || videoAssetIdStr.isEmpty()) {
                log.warn("Missing eventType or videoAssetId in playback event: {}", json);
                return;
            }
            PlaybackEventType eventType;
            try {
                eventType = PlaybackEventType.valueOf(eventTypeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown eventType in playback event: {}", eventTypeStr);
                return;
            }
            UUID videoAssetId = UUID.fromString(videoAssetIdStr);
            recordEvent(videoAssetId, eventType, userId, currentTimeSeconds, timestamp);
        } catch (Exception e) {
            log.error("Failed to parse or record playback event: {}", json, e);
        }
    }

    private static String extractJsonString(String json, String key) {
        String quoted = "\"" + key + "\":\"";
        int start = json.indexOf(quoted);
        if (start == -1) {
            quoted = "\"" + key + "\":";
            start = json.indexOf(quoted);
            if (start == -1)
                return null;
            start += quoted.length();
            int end = json.indexOf(",", start);
            if (end == -1)
                end = json.indexOf("}", start);
            if (end == -1)
                return null;
            String val = json.substring(start, end).trim();
            if (val.startsWith("\"") && val.endsWith("\""))
                return val.substring(1, val.length() - 1);
            return val.equals("null") ? null : val;
        }
        start += quoted.length();
        int end = start;
        while (end < json.length() && json.charAt(end) != '"') {
            if (json.charAt(end) == '\\')
                end++;
            end++;
        }
        return json.substring(start, end);
    }

    private static String extractJsonStringOrNull(String json, String key) {
        String quoted = "\"" + key + "\":";
        int start = json.indexOf(quoted);
        if (start == -1)
            return null;
        start += quoted.length();
        if (start < json.length() && json.substring(start).startsWith("null"))
            return null;
        if (start < json.length() && json.charAt(start) == '"') {
            start++;
            int end = start;
            while (end < json.length() && json.charAt(end) != '"') {
                if (json.charAt(end) == '\\')
                    end++;
                end++;
            }
            return end <= json.length() ? json.substring(start, end) : null;
        }
        return null;
    }

    private static Integer extractJsonIntOrNull(String json, String key) {
        String quoted = "\"" + key + "\":";
        int start = json.indexOf(quoted);
        if (start == -1)
            return null;
        start += quoted.length();
        if (start < json.length() && json.substring(start).startsWith("null"))
            return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'))
            end++;
        if (end == start)
            return null;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
