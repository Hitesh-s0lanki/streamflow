package com.streamflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/* Video variant entity */
@Getter
@Setter
@Entity
@Table(name = "video_variant", indexes = {
        @Index(name = "idx_video_variant_video_asset_id", columnList = "video_asset_id"),
        @Index(name = "idx_video_variant_resolution", columnList = "video_asset_id, resolution")
})
public class VideoVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "resolution", nullable = false, length = 32)
    private String resolution;

    @Column(name = "bitrate_kbps")
    private Integer bitrateKbps;

    @Column(name = "codec", length = 64)
    private String codec;

    @Column(name = "segment_path", length = 1024)
    private String segmentPath;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
