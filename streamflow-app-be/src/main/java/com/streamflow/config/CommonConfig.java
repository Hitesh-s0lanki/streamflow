package com.streamflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized common properties used across services.
 * Override in tests via application-test.properties or @TestPropertySource.
 */
@Configuration
@ConfigurationProperties(prefix = "streamflow.common")
public class CommonConfig {

    /** Analytics: default number of top videos in overview. */
    private int defaultTopVideos = 10;
    /** Analytics: max records per video analytics query. */
    private int maxAnalyticsPage = 500;

    /** Watch progress: seconds from end to auto-mark completed. */
    private int completionThresholdSeconds = 60;
    /** Watch progress: days to include in continue-watching. */
    private int continueWatchingDays = 30;
    /** Watch progress: max items in continue-watching list. */
    private int continueWatchingLimit = 50;

    public int getDefaultTopVideos() {
        return defaultTopVideos;
    }

    public void setDefaultTopVideos(int defaultTopVideos) {
        this.defaultTopVideos = defaultTopVideos;
    }

    public int getMaxAnalyticsPage() {
        return maxAnalyticsPage;
    }

    public void setMaxAnalyticsPage(int maxAnalyticsPage) {
        this.maxAnalyticsPage = maxAnalyticsPage;
    }

    public int getCompletionThresholdSeconds() {
        return completionThresholdSeconds;
    }

    public void setCompletionThresholdSeconds(int completionThresholdSeconds) {
        this.completionThresholdSeconds = completionThresholdSeconds;
    }

    public int getContinueWatchingDays() {
        return continueWatchingDays;
    }

    public void setContinueWatchingDays(int continueWatchingDays) {
        this.continueWatchingDays = continueWatchingDays;
    }

    public int getContinueWatchingLimit() {
        return continueWatchingLimit;
    }

    public void setContinueWatchingLimit(int continueWatchingLimit) {
        this.continueWatchingLimit = continueWatchingLimit;
    }
}
