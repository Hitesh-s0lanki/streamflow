import { apiFetch } from "./client";

export interface MediaUrlResponse {
  url: string;
  expiresAt: string;
  key: string;
}

/**
 * GET /api/media/url — resolve an S3 object key into a presigned URL.
 * Returns a time-limited public URL for the given key.
 */
export async function getMediaUrl(
  key: string,
  expirationMinutes = 60,
): Promise<MediaUrlResponse> {
  const params = new URLSearchParams({ key });
  if (expirationMinutes !== 60) {
    params.set("expirationMinutes", String(expirationMinutes));
  }
  return apiFetch<MediaUrlResponse>(`/api/media/url?${params.toString()}`);
}
