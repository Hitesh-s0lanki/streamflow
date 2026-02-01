/**
 * Upload flow API: content, seasons, episodes, video-assets, ingestion.
 * Uses JAVA_SERVER_URL (client: NEXT_PUBLIC_JAVA_SERVER_URL).
 */

import { apiFetch, apiPutBinary } from "./client";

// --- Request / Response types (align with backend DTOs) ---

export type ContentType = "MOVIE" | "SERIES";

export interface CreateContentRequest {
  title: string;
  contentType: ContentType;
  description?: string | null;
  releaseYear?: number | null;
  rating?: string | null;
  posterUrl?: string | null;
  thumbnailUrl?: string | null;
}

export interface ContentDetailResponse {
  id: string;
  title: string;
  description?: string | null;
  contentType: ContentType;
  releaseYear?: number | null;
  rating?: string | null;
  posterUrl?: string | null;
  thumbnailUrl?: string | null;
  publishStatus: string;
  durationSeconds?: number | null;
  createdAt: string;
  updatedAt: string;
  seasons?: SeasonSummaryResponse[];
}

export interface CreateSeasonRequest {
  seasonNumber: number;
  title?: string | null;
  posterUrl?: string | null;
}

export interface SeasonSummaryResponse {
  id: string;
  seasonNumber: number;
  title?: string | null;
  posterUrl?: string | null;
}

export interface CreateEpisodeRequest {
  episodeNumber: number;
  title: string;
  durationSeconds: number;
  description?: string | null;
  thumbnailUrl?: string | null;
}

export interface EpisodeListItemResponse {
  id: string;
  episodeNumber: number;
  title: string;
  durationSeconds: number;
  thumbnailUrl?: string | null;
}

export interface CreateVideoAssetRequest {
  durationSeconds: number;
  contentId?: string | null;
  episodeId?: string | null;
}

export interface VideoAssetResponse {
  id: string;
  contentId?: string | null;
  episodeId?: string | null;
  durationSeconds: number;
  drmEnabled?: boolean;
}

export interface UploadUrlResponse {
  uploadUrl: string;
  rawS3Key: string;
  expiration: string;
}

export interface ConfirmUploadRequest {
  rawS3Key: string;
  contentType?: string | null;
}

export type IngestionStatus =
  | "PENDING"
  | "UPLOADING"
  | "UPLOADED"
  | "PROCESSING"
  | "TRANSCODED"
  | "SPRITES_GENERATED"
  | "READY"
  | "FAILED";

export interface IngestionStatusResponse {
  jobId: string;
  videoAssetId: string;
  jobStatus: IngestionStatus;
  processedAt?: string | null;
  errorMessage?: string | null;
  rawS3Key?: string | null;
  createdAt?: string | null;
}

// --- API functions ---

export async function createContent(
  body: CreateContentRequest
): Promise<ContentDetailResponse> {
  return apiFetch<ContentDetailResponse>("/api/content", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function createSeason(
  contentId: string,
  body: CreateSeasonRequest
): Promise<SeasonSummaryResponse> {
  return apiFetch<SeasonSummaryResponse>(`/api/content/${contentId}/seasons`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function createEpisode(
  seasonId: string,
  body: CreateEpisodeRequest
): Promise<EpisodeListItemResponse> {
  return apiFetch<EpisodeListItemResponse>(`/api/seasons/${seasonId}/episodes`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function createVideoAsset(
  body: CreateVideoAssetRequest
): Promise<VideoAssetResponse> {
  return apiFetch<VideoAssetResponse>("/api/video-assets", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function getUploadUrl(
  videoAssetId: string
): Promise<UploadUrlResponse> {
  return apiFetch<UploadUrlResponse>(
    `/api/video-assets/${videoAssetId}/upload-url`,
    { method: "POST" }
  );
}

export async function uploadFileToPresignedUrl(
  uploadUrl: string,
  file: File
): Promise<void> {
  return apiPutBinary(uploadUrl, file);
}

export async function confirmUpload(
  videoAssetId: string,
  body: ConfirmUploadRequest
): Promise<IngestionStatusResponse> {
  return apiFetch<IngestionStatusResponse>(
    `/api/ingestion/${videoAssetId}/uploaded`,
    {
      method: "POST",
      body: JSON.stringify(body),
    }
  );
}

export async function getIngestionStatus(
  videoAssetId: string
): Promise<IngestionStatusResponse> {
  return apiFetch<IngestionStatusResponse>(
    `/api/ingestion/${videoAssetId}`,
    { method: "GET" }
  );
}
