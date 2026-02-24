import { z } from "zod";

/** Shared search result shape for both movie and series. */
export const ContentSearchSchema = z.object({
  matches: z.array(
    z.object({
      title: z.string(),
      year: z.number(),
    }),
  ),
  suggestion: z.array(z.string()).nullable(),
});

/** Shared details shape for both movie and series. */
export const ContentDetailsSchema = z.object({
  title: z.string(),
  description: z.string(),
  release_year: z.number(),
  rating: z.number(),
});

// Legacy aliases for backward compatibility
export const MovieSearchSchema = ContentSearchSchema;
export const MovieDetailsSchema = ContentDetailsSchema;

export type ContentDetails = z.infer<typeof ContentDetailsSchema>;
export type MovieDetails = ContentDetails;
