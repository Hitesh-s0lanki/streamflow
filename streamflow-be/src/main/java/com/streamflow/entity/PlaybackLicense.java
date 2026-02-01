package com.streamflow.entity;

import com.streamflow.entity.enums.LicenseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Temporary permission to decrypt/play a video. userId from Clerk.
 */
@Getter
@Setter
@Entity
@Table(name = "playback_license", indexes = {
        @Index(name = "idx_playback_license_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_playback_license_user_id", columnList = "user_id"),
        @Index(name = "idx_playback_license_user_asset", columnList = "user_id, video_asset_id"),
        @Index(name = "idx_playback_license_expires_at", columnList = "expires_at"),
        @Index(name = "idx_playback_license_status", columnList = "license_status")
})
public class PlaybackLicense extends BaseEntity {

    /** Clerk user id. */
    @Column(name = "user_id", nullable = false, length = 256)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_status", nullable = false, length = 32)
    private LicenseStatus licenseStatus = LicenseStatus.ACTIVE;

    @Column(name = "device_id", length = 256)
    private String deviceId;
}
