"use server";

import { apiFetch, ApiError, getApiBaseUrl } from "@/lib/api/client";
import type {
  CreateContentInput,
  CreateSeasonInput,
  ContentResponse,
  SeasonSummaryResponse,
  VideoUploadResponse,
  VideoProcessingResponse,
} from "./schema";

const CONTENT_API = "/api/content";

/**
 * Forward a FormData request to the Java backend.
 * Content-Type is intentionally omitted so fetch sets the multipart boundary.
 */
async function forwardFormData<T>(
  path: string,
  formData: FormData,
): Promise<T> {
  const base = getApiBaseUrl();
  const res = await fetch(`${base}${path}`, {
    method: "POST",
    body: formData,
  });

  if (!res.ok) {
    const text = await res.text();
    let message = `Request failed (${res.status})`;
    try {
      const body = JSON.parse(text);
      message = body.message || body.error || message;
    } catch {
      if (text) message = text;
    }
    throw new ApiError(message, res.status);
  }

  return res.json();
}

// ── 1. Create Content ────────────────────────────────────────────────────────

export async function createContent(
  input: CreateContentInput,
): Promise<ContentResponse> {
  return apiFetch<ContentResponse>(CONTENT_API, {
    method: "POST",
    body: JSON.stringify(input),
  });
}

// ── 2. Create Season (SERIES only) ───────────────────────────────────────────

export async function createSeason(
  input: CreateSeasonInput,
): Promise<SeasonSummaryResponse> {
  return apiFetch<SeasonSummaryResponse>(
    `${CONTENT_API}/${input.contentId}/seasons`,
    {
      method: "POST",
      body: JSON.stringify({
        seasonNumber: input.seasonNumber,
        title: input.title ?? undefined,
        posterUrl: input.posterUrl ?? undefined,
      }),
    },
  );
}

// ── 3. Upload Assets (poster & thumbnail) ────────────────────────────────────

export async function uploadAssets(
  contentId: string,
  formData: FormData,
): Promise<ContentResponse> {
  return forwardFormData<ContentResponse>(
    `${CONTENT_API}/${contentId}/assets`,
    formData,
  );
}

// ── 4. Upload Video ──────────────────────────────────────────────────────────

export async function uploadVideo(
  contentId: string,
  formData: FormData,
): Promise<VideoUploadResponse> {
  return forwardFormData<VideoUploadResponse>(
    `${CONTENT_API}/${contentId}/video`,
    formData,
  );
}

// ── 5. Get Video Upload Status ───────────────────────────────────────────────

export async function getVideoUploadStatus(
  contentId: string,
): Promise<VideoUploadResponse> {
  return apiFetch<VideoUploadResponse>(
    `${CONTENT_API}/${contentId}/video/status`,
  );
}

// ── 6. Abort Video Upload ────────────────────────────────────────────────────

export async function abortVideoUpload(contentId: string): Promise<void> {
  await apiFetch<void>(`${CONTENT_API}/${contentId}/video`, {
    method: "DELETE",
  });
}

// ── 7. Trigger Video Processing ──────────────────────────────────────────────
// POST /api/content/{id}/video/process — fire-and-forget trigger.
// The video file is already uploaded; backend starts async transcoding.
// Poll with getVideoProcessingStatus afterwards.

export async function triggerVideoProcessing(
  contentId: string,
): Promise<VideoProcessingResponse> {
  const base = getApiBaseUrl();
  const res = await fetch(`${base}${CONTENT_API}/${contentId}/video/process`, {
    method: "POST",
    body: new FormData(),
  });

  if (!res.ok) {
    const text = await res.text();
    let message = `Request failed (${res.status})`;
    try {
      const body = JSON.parse(text);
      message = body.message || body.error || message;
    } catch {
      if (text) message = text;
    }
    throw new ApiError(message, res.status);
  }

  return res.json();
}

// ── 8. Get Video Processing Status ───────────────────────────────────────────

export async function getVideoProcessingStatus(
  contentId: string,
): Promise<VideoProcessingResponse> {
  return apiFetch<VideoProcessingResponse>(
    `${CONTENT_API}/${contentId}/video/process/status`,
  );
}
