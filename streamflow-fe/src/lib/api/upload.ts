/**
 * Upload flow API: content creation, asset upload, video upload.
 * Uses JAVA_SERVER_URL (client: NEXT_PUBLIC_JAVA_SERVER_URL).
 */

import { apiFetch, getApiBaseUrl, ApiError } from "./client";

// --- Request / Response types (align with backend DTOs) ---

export type ContentType = "MOVIE" | "SERIES";

export type PublishStatus = "DRAFT" | "PUBLISHED" | "ARCHIVED";

export type UploadStatus =
  | "PENDING"
  | "UPLOADING"
  | "MULTIPART_INITIATED"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export interface CreateContentRequest {
  title: string;
  contentType: ContentType;
  description?: string;
  releaseYear?: number;
  rating?: string;
  durationSeconds?: number;
}

export interface ContentResponse {
  id: string;
  title: string;
  description: string | null;
  contentType: ContentType;
  releaseYear: number | null;
  rating: string | null;
  durationSeconds: number | null;
  publishStatus: PublishStatus;
  posterUrl: string | null;
  thumbnailUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VideoUploadResponse {
  contentId: string;
  videoAssetId: string;
  uploadStatus: UploadStatus;
  originalFilename: string;
  fileSizeBytes: number;
  rawS3Key: string | null;
  uploadStartedAt: string | null;
  uploadCompletedAt: string | null;
  errorMessage: string | null;
  totalParts: number | null;
  uploadedParts: number | null;
  progressPercent: number | null;
  message: string;
}

export interface UploadErrorResponse {
  error: string;
  message: string;
  correlationId?: string;
  phase?: string;
  contentId?: string;
  videoAssetId?: string;
  uploadId?: string;
}

// --- API functions ---

/**
 * Step 1: Create content entry with DRAFT status
 * POST /api/content
 */
export async function createContent(
  body: CreateContentRequest,
): Promise<ContentResponse> {
  return apiFetch<ContentResponse>("/api/content", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

/**
 * Step 2: Upload poster and thumbnail assets
 * POST /api/content/{id}/assets
 */
export async function uploadAssets(
  contentId: string,
  poster: File,
  thumbnail: File,
  onProgress?: (progress: number) => void,
): Promise<ContentResponse> {
  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/content/${contentId}/assets`;

  const formData = new FormData();
  formData.append("poster", poster);
  formData.append("thumbnail", thumbnail);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", url);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const percentComplete = Math.round((event.loaded / event.total) * 100);
        onProgress(percentComplete);
      }
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          resolve(response);
        } catch {
          reject(new ApiError("Failed to parse response", xhr.status));
        }
      } else {
        try {
          const errorBody = JSON.parse(xhr.responseText);
          reject(
            new ApiError(
              errorBody.message ||
                errorBody.error ||
                `Upload failed: ${xhr.status}`,
              xhr.status,
              errorBody,
            ),
          );
        } catch {
          reject(
            new ApiError(
              `Upload failed: ${xhr.status} ${xhr.statusText}`,
              xhr.status,
            ),
          );
        }
      }
    };

    xhr.onerror = () => {
      reject(new ApiError("Network error during asset upload", 0));
    };

    xhr.send(formData);
  });
}

/**
 * Step 3: Upload video file
 * POST /api/content/{id}/video
 * Server automatically handles multipart upload for large files (>100MB)
 */
export async function uploadVideo(
  contentId: string,
  videoFile: File,
  onProgress?: (progress: number) => void,
): Promise<VideoUploadResponse> {
  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/content/${contentId}/video`;

  const formData = new FormData();
  formData.append("video", videoFile);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", url);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const percentComplete = Math.round((event.loaded / event.total) * 100);
        onProgress(percentComplete);
      }
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          resolve(response);
        } catch {
          reject(new ApiError("Failed to parse response", xhr.status));
        }
      } else {
        try {
          const errorBody = JSON.parse(xhr.responseText);
          reject(
            new ApiError(
              errorBody.message ||
                errorBody.error ||
                `Upload failed: ${xhr.status}`,
              xhr.status,
              errorBody,
            ),
          );
        } catch {
          reject(
            new ApiError(
              `Upload failed: ${xhr.status} ${xhr.statusText}`,
              xhr.status,
            ),
          );
        }
      }
    };

    xhr.onerror = () => {
      reject(new ApiError("Network error during video upload", 0));
    };

    xhr.send(formData);
  });
}

/**
 * Get video upload status
 * GET /api/content/{id}/video/status
 */
export async function getVideoUploadStatus(
  contentId: string,
): Promise<VideoUploadResponse> {
  return apiFetch<VideoUploadResponse>(
    `/api/content/${contentId}/video/status`,
    {
      method: "GET",
    },
  );
}

/**
 * Retry failed video upload
 * POST /api/content/{id}/video/retry
 */
export async function retryVideoUpload(
  contentId: string,
  videoFile: File,
  onProgress?: (progress: number) => void,
): Promise<VideoUploadResponse> {
  const baseUrl = getApiBaseUrl();
  const url = `${baseUrl}/api/content/${contentId}/video/retry`;

  const formData = new FormData();
  formData.append("video", videoFile);

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", url);

    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable && onProgress) {
        const percentComplete = Math.round((event.loaded / event.total) * 100);
        onProgress(percentComplete);
      }
    };

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try {
          const response = JSON.parse(xhr.responseText);
          resolve(response);
        } catch {
          reject(new ApiError("Failed to parse response", xhr.status));
        }
      } else {
        try {
          const errorBody = JSON.parse(xhr.responseText);
          reject(
            new ApiError(
              errorBody.message ||
                errorBody.error ||
                `Retry failed: ${xhr.status}`,
              xhr.status,
              errorBody,
            ),
          );
        } catch {
          reject(
            new ApiError(
              `Retry failed: ${xhr.status} ${xhr.statusText}`,
              xhr.status,
            ),
          );
        }
      }
    };

    xhr.onerror = () => {
      reject(new ApiError("Network error during video upload retry", 0));
    };

    xhr.send(formData);
  });
}

/**
 * Abort video upload
 * DELETE /api/content/{id}/video/abort
 */
export async function abortVideoUpload(contentId: string): Promise<void> {
  return apiFetch<void>(`/api/content/${contentId}/video/abort`, {
    method: "DELETE",
  });
}
