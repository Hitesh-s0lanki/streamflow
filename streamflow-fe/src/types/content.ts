/**
 * Types matching backend DTOs for catalog and content detail.
 * @see doc/catalog-pages.md, doc/content-detail.md
 */

export type ContentType = "MOVIE" | "SERIES";

export type PublishStatus = "DRAFT" | "PUBLISHED";

/** GET /api/content — catalog listing (minimal fields for cards/grids). */
export interface ContentCatalogItem {
  id: string;
  title: string;
  contentType: ContentType;
  thumbnailUrl: string | null;
  publishStatus: PublishStatus;
  releaseYear: number | null;
  createdAt: string;
}

/** GET /api/content/{contentId} — full content detail (for hero/detail page). */
export interface ContentDetail {
  id: string;
  title: string;
  description: string | null;
  contentType: ContentType;
  releaseYear: number | null;
  rating: string | null;
  posterUrl: string | null;
  thumbnailUrl: string | null;
  publishStatus: PublishStatus;
  durationSeconds: number | null;
  createdAt: string;
  updatedAt: string;
}

/** GET /api/watch-progress/continue — continue watching row item. */
export interface ContinueWatchingItem {
  videoAssetId: string;
  contentId: string;
  episodeId: string | null;
  title: string;
  episodeTitle: string | null;
  posterUrl: string | null;
  thumbnailUrl: string | null;
  lastWatchedSecond: number;
  durationSeconds: number;
  lastWatchedAt: string;
}
