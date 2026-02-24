import { z } from "zod";

// ── Enums ────────────────────────────────────────────────────────────────────

export const ContentTypeEnum = z.enum(["MOVIE", "SERIES"]);
export const PublishStatusEnum = z.enum(["DRAFT", "PUBLISHED"]);

export const UploadStatusEnum = z.enum([
  "PENDING",
  "MULTIPART_INITIATED",
  "UPLOADING",
  "COMPLETED",
  "FAILED",
  "CANCELLED",
]);

export const ProcessingStatusEnum = z.enum([
  "NONE",
  "QUEUED",
  "PROCESSING",
  "COMPLETED",
  "FAILED",
]);

export const TERMINAL_UPLOAD_STATUSES: ReadonlySet<string> = new Set([
  "COMPLETED",
  "FAILED",
  "CANCELLED",
]);

export const TERMINAL_PROCESSING_STATUSES: ReadonlySet<string> = new Set([
  "COMPLETED",
  "FAILED",
]);

// ── Input Schemas ────────────────────────────────────────────────────────────

export const CreateContentSchema = z.object({
  title: z
    .string()
    .min(1, "Title is required")
    .max(512, "Title must be at most 512 characters"),
  description: z.string().nullish(),
  contentType: ContentTypeEnum,
  releaseYear: z.coerce.number().int().positive().nullish(),
  rating: z
    .string()
    .max(16, "Rating must be at most 16 characters")
    .nullish(),
  durationSeconds: z.coerce.number().int().positive().nullish(),
});

export const ContentIdSchema = z.object({
  contentId: z.string().uuid("Invalid content ID"),
});

export const CreateSeasonSchema = z.object({
  contentId: z.string().uuid("Invalid content ID"),
  seasonNumber: z.coerce.number().int().positive(),
  title: z.string().max(512).optional(),
  posterUrl: z.string().max(1024).optional(),
});

export const SeasonSummarySchema = z.object({
  id: z.string(),
  seasonNumber: z.number(),
  title: z.string().nullable(),
  posterUrl: z.string().nullable(),
});

// ── Response Schemas ─────────────────────────────────────────────────────────

export const ContentResponseSchema = z.object({
  id: z.string(),
  title: z.string(),
  description: z.string().nullable(),
  contentType: ContentTypeEnum,
  releaseYear: z.number().nullable(),
  rating: z.string().nullable(),
  durationSeconds: z.number().nullable(),
  publishStatus: PublishStatusEnum,
  posterUrl: z.string().nullable(),
  thumbnailUrl: z.string().nullable(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const VideoUploadResponseSchema = z.object({
  contentId: z.string(),
  videoAssetId: z.string(),
  uploadStatus: UploadStatusEnum,
  originalFilename: z.string(),
  fileSizeBytes: z.number(),
  rawS3Key: z.string().nullable(),
  uploadStartedAt: z.string().nullable(),
  uploadCompletedAt: z.string().nullable(),
  errorMessage: z.string().nullable(),
  totalParts: z.number().nullable(),
  uploadedParts: z.number().nullable(),
  progressPercent: z.number().nullable(),
  message: z.string(),
});

export const VideoProcessingResponseSchema = z.object({
  contentId: z.string(),
  videoAssetId: z.string(),
  processingStatus: ProcessingStatusEnum,
  processingStartedAt: z.string().nullable(),
  processingCompletedAt: z.string().nullable(),
  errorMessage: z.string().nullable(),
  message: z.string(),
});

// ── Inferred Types ───────────────────────────────────────────────────────────

export type CreateContentInput = z.infer<typeof CreateContentSchema>;
export type CreateSeasonInput = z.infer<typeof CreateSeasonSchema>;
export type SeasonSummaryResponse = z.infer<typeof SeasonSummarySchema>;
export type ContentResponse = z.infer<typeof ContentResponseSchema>;
export type VideoUploadResponse = z.infer<typeof VideoUploadResponseSchema>;
export type VideoProcessingResponse = z.infer<
  typeof VideoProcessingResponseSchema
>;
export type UploadStatus = z.infer<typeof UploadStatusEnum>;
export type ProcessingStatus = z.infer<typeof ProcessingStatusEnum>;
