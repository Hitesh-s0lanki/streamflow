"use server";

import { zodTextFormat } from "openai/helpers/zod";
import { openai } from "@/lib/openai";
import {
  ContentSearchSchema,
  ContentDetailsSchema,
  type ContentDetails,
} from "./schema";

export type ContentTypeKind = "MOVIE" | "SERIES";

// ── 1. Search ─────────────────────────────────────────────────────────────────

/** Movie-only search. Use for MOVIE content type. */
export async function searchMovie(title: string) {
  const response = await openai.responses.parse({
    model: process.env.OPENAI_MODEL_MINI!,
    input: `Search the web for a movie titled "${title}". 
    If exact match exists return matches array with one item.
    If multiple close matches exist return them in matches.
    If no direct match exists return empty matches and provide suggestion array.`,
    tools: [{ type: "web_search_preview" }],
    text: {
      format: zodTextFormat(ContentSearchSchema, "movie_search"),
    },
  });

  return response.output_parsed;
}

/** Series-only search. Use for SERIES content type. */
export async function searchContent(
  title: string,
  contentType: ContentTypeKind,
) {
  if (contentType === "MOVIE") {
    return searchMovie(title);
  }
  const response = await openai.responses.parse({
    model: process.env.OPENAI_MODEL_MINI!,
    input: `Search the web for a TV series titled "${title}". 
    If exact match exists return matches array with one item.
    If multiple close matches exist return them in matches.
    If no direct match exists return empty matches and provide suggestion array.`,
    tools: [{ type: "web_search_preview" }],
    text: {
      format: zodTextFormat(ContentSearchSchema, "content_search"),
    },
  });

  return response.output_parsed;
}

// ── 2. Get Details ───────────────────────────────────────────────────────────

/** Movie-only details. Use for MOVIE content type. */
export async function getMovieDetails(movieName: string) {
  const response = await openai.responses.parse({
    model: process.env.OPENAI_MODEL_FULL!,
    input: `Provide detailed verified movie information for "${movieName}".
    Include description, release year, and IMDb-style rating.`,
    tools: [{ type: "web_search_preview" }],
    text: {
      format: zodTextFormat(ContentDetailsSchema, "movie_details"),
    },
  });

  return response.output_parsed;
}

/** Series-only details. Use for SERIES content type. */
export async function getContentDetails(
  name: string,
  contentType: ContentTypeKind,
) {
  if (contentType === "MOVIE") {
    return getMovieDetails(name);
  }
  const response = await openai.responses.parse({
    model: process.env.OPENAI_MODEL_FULL!,
    input: `Provide detailed verified TV series information for "${name}".
    Include description, release year, and IMDb-style rating.`,
    tools: [{ type: "web_search_preview" }],
    text: {
      format: zodTextFormat(ContentDetailsSchema, "content_details"),
    },
  });

  return response.output_parsed;
}

// ── 3. Generate Images (parallel) ───────────────────────────────────────────

/** Movie-only image generation. Use for MOVIE content type. */
export async function generateMovieImages(movie: ContentDetails) {
  const posterPrompt = `Create a cinematic high-quality movie poster for:
Title: ${movie.title}
Description: ${movie.description}
Vertical format. Professional Hollywood poster style.`;

  const thumbnailPrompt = `Create a cinematic landscape thumbnail for:
Title: ${movie.title}
Description: ${movie.description}
Wide 16:9 YouTube-style thumbnail.`;

  const [poster, thumbnail] = await Promise.all([
    openai.images.generate({
      model: process.env.OPENAI_MODEL_IMAGE!,
      prompt: posterPrompt,
      size: "1024x1536",
    }),
    openai.images.generate({
      model: process.env.OPENAI_MODEL_IMAGE!,
      prompt: thumbnailPrompt,
      size: "1536x1024",
    }),
  ]);

  return {
    poster: `data:image/png;base64,${poster.data![0].b64_json}`,
    thumbnail: `data:image/png;base64,${thumbnail.data![0].b64_json}`,
  };
}

/** Series (and generic) image generation. Use for SERIES content type. */
export async function generateContentImages(content: ContentDetails) {
  const posterPrompt = `Create a cinematic high-quality poster for:
Title: ${content.title}
Description: ${content.description}
Vertical format. Professional Hollywood poster style.`;

  const thumbnailPrompt = `Create a cinematic landscape thumbnail for:
Title: ${content.title}
Description: ${content.description}
Wide 16:9 YouTube-style thumbnail.`;

  const [poster, thumbnail] = await Promise.all([
    openai.images.generate({
      model: process.env.OPENAI_MODEL_IMAGE!,
      prompt: posterPrompt,
      size: "1024x1536",
    }),
    openai.images.generate({
      model: process.env.OPENAI_MODEL_IMAGE!,
      prompt: thumbnailPrompt,
      size: "1536x1024",
    }),
  ]);

  return {
    poster: `data:image/png;base64,${poster.data![0].b64_json}`,
    thumbnail: `data:image/png;base64,${thumbnail.data![0].b64_json}`,
  };
}
