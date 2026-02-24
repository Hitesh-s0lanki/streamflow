import { createTRPCRouter, protectedProcedure } from "@/trpc/init";
import { TRPCError } from "@trpc/server";
import { z } from "zod";
import {
  searchMovie as searchMovieAction,
  searchContent as searchContentAction,
  getMovieDetails as getMovieDetailsAction,
  getContentDetails as getContentDetailsAction,
  generateMovieImages as generateMovieImagesAction,
  generateContentImages as generateContentImagesAction,
} from "../actions";
import { ContentDetailsSchema } from "../schema";

const contentTypeEnum = z.enum(["MOVIE", "SERIES"]);

export const aiRouter = createTRPCRouter({
  searchContent: protectedProcedure
    .input(
      z.object({
        title: z.string().min(1),
        contentType: contentTypeEnum,
      }),
    )
    .mutation(async ({ input }) => {
      try {
        const result = await searchContentAction(
          input.title,
          input.contentType,
        );
        if (!result) {
          throw new TRPCError({
            code: "INTERNAL_SERVER_ERROR",
            message: "AI service did not return a valid response.",
          });
        }
        return result;
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI searchContent failed:", err);
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Could not search for the content.",
        });
      }
    }),

  getContentDetails: protectedProcedure
    .input(
      z.object({
        name: z.string().min(1),
        contentType: contentTypeEnum,
      }),
    )
    .mutation(async ({ input }) => {
      try {
        const result = await getContentDetailsAction(
          input.name,
          input.contentType,
        );
        if (!result) {
          throw new TRPCError({
            code: "INTERNAL_SERVER_ERROR",
            message: "AI service did not return content details.",
          });
        }
        return result;
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI getContentDetails failed:", err);
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Could not fetch content details.",
        });
      }
    }),

  generateContentImages: protectedProcedure
    .input(ContentDetailsSchema)
    .mutation(async ({ input }) => {
      try {
        const result = await generateContentImagesAction(input);
        return result;
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI generateContentImages failed:", err);
        throw new TRPCError({
          code: "INTERNAL_SERVER_ERROR",
          message: "Failed to generate content images.",
        });
      }
    }),

  // Movie APIs (dedicated movie-only)
  searchMovie: protectedProcedure
    .input(z.object({ title: z.string().min(1) }))
    .mutation(async ({ input }) => {
      try {
        const result = await searchMovieAction(input.title);
        if (!result) {
          throw new TRPCError({
            code: "INTERNAL_SERVER_ERROR",
            message: "AI service did not return a valid response.",
          });
        }
        return result;
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI searchMovie failed:", err);
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Could not search for the movie.",
        });
      }
    }),

  getMovieDetails: protectedProcedure
    .input(z.object({ movieName: z.string().min(1) }))
    .mutation(async ({ input }) => {
      try {
        const result = await getMovieDetailsAction(input.movieName);
        if (!result) {
          throw new TRPCError({
            code: "INTERNAL_SERVER_ERROR",
            message: "AI service did not return movie details.",
          });
        }
        return result;
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI getMovieDetails failed:", err);
        throw new TRPCError({
          code: "BAD_REQUEST",
          message: "Could not fetch movie details.",
        });
      }
    }),

  generateMovieImages: protectedProcedure
    .input(ContentDetailsSchema)
    .mutation(async ({ input }) => {
      try {
        return await generateMovieImagesAction(input);
      } catch (err) {
        if (err instanceof TRPCError) throw err;
        console.error("AI generateMovieImages failed:", err);
        throw new TRPCError({
          code: "INTERNAL_SERVER_ERROR",
          message: "Failed to generate movie images.",
        });
      }
    }),
});
