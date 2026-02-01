/**
 * Types matching backend DTOs for catalog and content detail.
 * @see doc/catalog-pages.md, doc/content-detail.md
 */

export type ContentType = "MOVIE" | "SERIES";

/** GET /api/content — catalog listing (PUBLISHED only). */
export interface ContentCatalogItem {
  id: string;
  title: string;
  contentType: ContentType;
  posterUrl: string | null;
  thumbnailUrl: string | null;
  releaseYear: number | null;
  durationSeconds: number | null;
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
  publishStatus: string;
  durationSeconds: number | null;
  createdAt: string;
  updatedAt: string;
  seasons?: unknown[];
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
