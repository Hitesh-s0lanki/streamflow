import { z } from "zod";
import { createTRPCRouter, protectedProcedure } from "@/trpc/init";
import { TRPCError } from "@trpc/server";
import { ApiError } from "@/lib/api/client";
import {
  createContent,
  createSeason,
  uploadAssets,
  getVideoUploadStatus,
  abortVideoUpload,
  triggerVideoProcessing,
  getVideoProcessingStatus,
} from "../actions";
import {
  CreateContentSchema,
  CreateSeasonSchema,
  ContentIdSchema,
} from "../schema";

// ── Error mapping ────────────────────────────────────────────────────────────

type TRPCCode = ConstructorParameters<typeof TRPCError>[0]["code"];

function httpToTRPCCode(status: number): TRPCCode {
  switch (status) {
    case 400:
      return "BAD_REQUEST";
    case 404:
      return "NOT_FOUND";
    case 409:
      return "CONFLICT";
    default:
      return "INTERNAL_SERVER_ERROR";
  }
}

function rethrow(err: unknown, fallback: string): never {
  if (err instanceof TRPCError) throw err;
  if (err instanceof ApiError) {
    throw new TRPCError({
      code: httpToTRPCCode(err.status),
      message: err.message,
    });
  }
  const message = err instanceof Error ? err.message : fallback;
  throw new TRPCError({ code: "INTERNAL_SERVER_ERROR", message });
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function base64ToBlob(dataUrl: string): Blob {
  const [header, b64] = dataUrl.split(",");
  const mime = header?.match(/:(.*?);/)?.[1] ?? "application/octet-stream";
  const bytes = Buffer.from(b64!, "base64");
  return new Blob([bytes], { type: mime });
}

// ── Router ───────────────────────────────────────────────────────────────────

export const contentRouter = createTRPCRouter({
  create: protectedProcedure
    .input(CreateContentSchema)
    .mutation(async ({ input }) => {
      try {
        return await createContent(input);
      } catch (err) {
        rethrow(err, "Failed to create content.");
      }
    }),

  createSeason: protectedProcedure
    .input(CreateSeasonSchema)
    .mutation(async ({ input }) => {
      try {
        return await createSeason(input);
      } catch (err) {
        rethrow(err, "Failed to create season.");
      }
    }),

  uploadAssets: protectedProcedure
    .input(
      ContentIdSchema.extend({
        posterBase64: z.string().min(1, "Poster image is required"),
        thumbnailBase64: z.string().min(1, "Thumbnail image is required"),
      }),
    )
    .mutation(async ({ input }) => {
      try {
        const formData = new FormData();
        formData.append(
          "poster",
          base64ToBlob(input.posterBase64),
          "poster.png",
        );
        formData.append(
          "thumbnail",
          base64ToBlob(input.thumbnailBase64),
          "thumbnail.png",
        );
        return await uploadAssets(input.contentId, formData);
      } catch (err) {
        rethrow(err, "Failed to upload assets.");
      }
    }),

  getUploadStatus: protectedProcedure
    .input(ContentIdSchema)
    .query(async ({ input }) => {
      try {
        return await getVideoUploadStatus(input.contentId);
      } catch (err) {
        rethrow(err, "Failed to fetch upload status.");
      }
    }),

  abortUpload: protectedProcedure
    .input(ContentIdSchema)
    .mutation(async ({ input }) => {
      try {
        await abortVideoUpload(input.contentId);
        return { success: true as const };
      } catch (err) {
        rethrow(err, "Failed to abort video upload.");
      }
    }),

  triggerProcessing: protectedProcedure
    .input(ContentIdSchema)
    .mutation(async ({ input }) => {
      try {
        return await triggerVideoProcessing(input.contentId);
      } catch (err) {
        rethrow(err, "Failed to trigger video processing.");
      }
    }),

  getProcessingStatus: protectedProcedure
    .input(ContentIdSchema)
    .query(async ({ input }) => {
      try {
        return await getVideoProcessingStatus(input.contentId);
      } catch (err) {
        rethrow(err, "Failed to fetch processing status.");
      }
    }),
});
