/**
 * Content catalog entity types.
 * Content (root), SeriesSeason, Episode — sub-entities under Content.
 */

import type { BaseEntity } from "../utils/types";
import type { ContentType, PublishStatus } from "../utils/types";
import type { VideoAsset } from "../video-asset/types";

// --- Content (parent) ---

/**
 * Root catalog entity: a Movie or Series visible in the UI (home, search, preview).
 * Extends BaseEntity. Table: content.
 */
export interface Content extends BaseEntity {
  /** Max 512 chars */
  title: string;
  description?: string | null;
  /** MOVIE or SERIES */
  contentType: ContentType;
  releaseYear?: number | null;
  /** Max 16 chars */
  rating?: string | null;
  /** Max 1024 chars */
  posterUrl?: string | null;
  /** Max 1024 chars */
  thumbnailUrl?: string | null;
  /** Default: DRAFT */
  publishStatus: PublishStatus;
  /** For MOVIE. For SERIES, derived from episodes */
  durationSeconds?: number | null;
  /** One-to-many (ordered by seasonNumber). Only when contentType = SERIES */
  seasons?: SeriesSeason[];
  /** One-to-one. For MOVIE: single asset. For SERIES: use Episode → VideoAsset */
  videoAsset?: VideoAsset | null;
}

// --- SeriesSeason (sub-entity of Content) ---

/**
 * A season within a Series. Only used when Content.contentType = SERIES.
 * Extends BaseEntity. Table: series_season.
 */
export interface SeriesSeason extends BaseEntity {
  content?: Content | null;
  seasonNumber: number;
  /** Max 512 chars */
  title?: string | null;
  /** Max 1024 chars */
  posterUrl?: string | null;
  /** One-to-many (ordered by episodeNumber) */
  episodes?: Episode[];
}

// --- Episode (sub-entity of SeriesSeason) ---

/**
 * A single episode of a series. Each episode has one playable VideoAsset.
 * Extends BaseEntity. Table: episode.
 */
export interface Episode extends BaseEntity {
  season?: SeriesSeason | null;
  episodeNumber: number;
  /** Max 512 chars */
  title: string;
  description?: string | null;
  /** Duration in seconds */
  durationSeconds: number;
  /** Max 1024 chars */
  thumbnailUrl?: string | null;
  /** One-to-one */
  videoAsset?: VideoAsset | null;
}
