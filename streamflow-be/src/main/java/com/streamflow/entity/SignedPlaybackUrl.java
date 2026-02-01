package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Short-lived signed URL record for manifests/segments. Optional, for
 * auditing/debugging.
 */
@Getter
@Setter
@Entity
@Table(name = "signed_playback_url", indexes = {
        @Index(name = "idx_signed_playback_url_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_signed_playback_url_expires_at", columnList = "expires_at")
})
public class SignedPlaybackUrl extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "signed_url", nullable = false, columnDefinition = "TEXT")
    private String signedUrl;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "url_type", length = 32)
    private String urlType;
}
