/**
 * Video-asset and related entity types.
 * VideoAsset (parent) and sub-entities: VideoVariant, SpriteSheet, SpriteFrameMetadata,
 * IngestionJob, PlaybackLicense, SignedPlaybackUrl, WatchProgress, PlaybackEventLog, PlaybackAnalytics.
 */

import type { Content, Episode } from "../content/types";
import type { BaseEntity } from "../utils/types";
import type {
  IngestionStatus,
  LicenseStatus,
  PlaybackEventType,
} from "../utils/types";

// --- VideoAsset (parent) ---

/**
 * A single playable video unit. Movie → 1 VideoAsset; Episode → 1 VideoAsset.
 * Extends BaseEntity. Table: video_asset.
 */
export interface VideoAsset extends BaseEntity {
  /** Many-to-one. For MOVIE: the content. For SERIES: the series (Content). */
  content?: Content | null;
  /** One-to-one. For SERIES episode; null for movie. */
  episode?: Episode | null;
  /** Duration in seconds */
  durationSeconds: number;
  /** Max 1024 chars */
  manifestUrl?: string | null;
  /** Default: false */
  drmEnabled: boolean;
  /** One-to-many (ABR renditions) */
  variants?: VideoVariant[];
  /** One-to-many (preview thumbnails) */
  spriteSheets?: SpriteSheet[];
  /** One-to-many */
  ingestionJobs?: IngestionJob[];
}

// --- VideoVariant (sub-entity of VideoAsset) ---

/**
 * One ABR rendition of a video (resolution, bitrate, codec, segment path).
 * Extends BaseEntity. Table: video_variant.
 */
export interface VideoVariant extends BaseEntity {
  videoAsset?: VideoAsset | null;
  /** Max 32 chars */
  resolution: string;
  bitrateKbps?: number | null;
  /** Max 64 chars */
  codec?: string | null;
  /** Max 1024 chars */
  segmentPath?: string | null;
  sortOrder?: number | null;
}

// --- SpriteSheet (sub-entity of VideoAsset) ---

/**
 * One sprite image containing many preview frames for Netflix-style seek preview.
 * Extends BaseEntity. Table: sprite_sheet.
 */
export interface SpriteSheet extends BaseEntity {
  videoAsset?: VideoAsset | null;
  /** Max 1024 chars */
  spriteUrl: string;
  startTimeSeconds: number;
  endTimeSeconds: number;
  columns: number;
  rows: number;
  thumbnailWidth: number;
  thumbnailHeight: number;
  intervalSeconds?: number | null;
  /** One-to-many */
  frameMetadata?: SpriteFrameMetadata[];
}

// --- SpriteFrameMetadata (sub-entity of SpriteSheet) ---

/**
 * Per-frame coordinates and time offset for a sprite sheet (advanced preview mapping).
 * Extends BaseEntity. Table: sprite_frame_metadata.
 */
export interface SpriteFrameMetadata extends BaseEntity {
  spriteSheet?: SpriteSheet | null;
  frameIndex: number;
  timeOffsetSeconds: number;
  xPosition?: number | null;
  yPosition?: number | null;
  width?: number | null;
  height?: number | null;
}

// --- IngestionJob (sub-entity of VideoAsset) ---

/**
 * Tracks ingestion lifecycle for a video upload: raw S3 key, processing state, errors.
 * Extends BaseEntity. Table: ingestion_job.
 */
export interface IngestionJob extends BaseEntity {
  videoAsset?: VideoAsset | null;
  /** Default: PENDING */
  jobStatus: IngestionStatus;
  /** Max 1024 chars */
  rawS3Key?: string | null;
  errorMessage?: string | null;
  processedAt?: string | null;
}

// --- PlaybackLicense (sub-entity of VideoAsset) ---

/**
 * Temporary permission to decrypt/play a video. userId from Clerk.
 * Extends BaseEntity. Table: playback_license.
 */
export interface PlaybackLicense extends BaseEntity {
  /** Clerk user id, max 256 chars */
  userId: string;
  videoAsset?: VideoAsset | null;
  /** ISO-8601 */
  expiresAt: string;
  /** Default: ACTIVE */
  licenseStatus: LicenseStatus;
  /** Max 256 chars */
  deviceId?: string | null;
}

// --- SignedPlaybackUrl (sub-entity of VideoAsset) ---

/**
 * Short-lived signed URL record for manifests/segments. Optional, for auditing/debugging.
 * Extends BaseEntity. Table: signed_playback_url.
 */
export interface SignedPlaybackUrl extends BaseEntity {
  videoAsset?: VideoAsset | null;
  signedUrl: string;
  /** ISO-8601 */
  expiresAt: string;
  /** Max 32 chars */
  urlType?: string | null;
}

// --- WatchProgress (sub-entity of VideoAsset) ---

/**
 * Resume position per user (Clerk userId) and video asset.
 * Extends BaseEntity. Table: watch_progress.
 */
export interface WatchProgress extends BaseEntity {
  /** Max 256 chars */
  userId: string;
  videoAsset?: VideoAsset | null;
  /** Default: 0 */
  lastWatchedSecond: number;
  /** Default: false */
  completed: boolean;
  /** ISO-8601 */
  lastWatchedAt?: string | null;
}

// --- PlaybackEventLog (sub-entity of VideoAsset) ---

/**
 * Sampled playback events for debugging; full stream goes to Kafka.
 * Extends BaseEntity. Table: playback_event_log.
 */
export interface PlaybackEventLog extends BaseEntity {
  /** Max 256 chars */
  userId?: string | null;
  videoAsset?: VideoAsset | null;
  eventType: PlaybackEventType;
  currentTimeSeconds?: number | null;
  payload?: string | null;
}

// --- PlaybackAnalytics (sub-entity of VideoAsset) ---

/**
 * Aggregated playback metrics derived from Kafka consumers (optional).
 * Extends BaseEntity. Table: playback_analytics.
 */
export interface PlaybackAnalytics extends BaseEntity {
  videoAsset?: VideoAsset | null;
  /** ISO-8601 */
  periodStart: string;
  /** ISO-8601 */
  periodEnd: string;
  totalPlays?: number | null;
  uniqueViewers?: number | null;
  avgWatchTimeSeconds?: number | null;
  /** precision 5, scale 4 */
  completionRate?: number | null;
  /** precision 5, scale 4 */
  bufferingRate?: number | null;
}
