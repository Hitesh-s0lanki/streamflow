"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTRPC } from "@/trpc/client";
import { toast } from "sonner";
import type { z } from "zod";
import type {
  ContentSearchSchema,
  ContentDetailsSchema,
} from "@/modules/ai/schema";

export type ContentTypeKind = "MOVIE" | "SERIES";

export type ContentMatch = { title: string; year: number };
export type ContentSearchResult = z.infer<typeof ContentSearchSchema>;
export type ContentDetails = z.infer<typeof ContentDetailsSchema>;
export type ContentImages = { poster: string; thumbnail: string };

export type MovieMatch = ContentMatch;
export type MovieSearchResult = ContentSearchResult;
export type MovieDetails = ContentDetails;
export type MovieImages = ContentImages;

export type GenerationPhase =
  | "search"
  | "select"
  | "generating"
  | "review"
  | "saving"
  | "upload";

export function useContentGeneration() {
  const trpc = useTRPC();

  const [phase, setPhase] = useState<GenerationPhase>("search");
  const [contentType, setContentType] = useState<ContentTypeKind>("MOVIE");
  const [season, setSeason] = useState<number | null>(null);
  const [selectedMatch, setSelectedMatch] = useState<ContentMatch | null>(null);
  const [contentDetails, setContentDetails] = useState<ContentDetails | null>(null);
  const [contentImages, setContentImages] = useState<ContentImages | null>(null);
  const [contentId, setContentId] = useState<string | null>(null);
  const [imageGenFailed, setImageGenFailed] = useState(false);

  // ── AI mutations: movie APIs for MOVIE, content APIs for SERIES ─────────

  const searchMovieMutation = useMutation(trpc.ai.searchMovie.mutationOptions());
  const searchContentMutation = useMutation(
    trpc.ai.searchContent.mutationOptions(),
  );
  const movieDetailsMutation = useMutation(
    trpc.ai.getMovieDetails.mutationOptions(),
  );
  const contentDetailsMutation = useMutation(
    trpc.ai.getContentDetails.mutationOptions(),
  );
  const movieImagesMutation = useMutation(
    trpc.ai.generateMovieImages.mutationOptions(),
  );
  const contentImagesMutation = useMutation(
    trpc.ai.generateContentImages.mutationOptions(),
  );

  // ── Content mutations ─────────────────────────────────────────────────────

  const createContentMutation = useMutation(
    trpc.content.create.mutationOptions(),
  );
  const createSeasonMutation = useMutation(
    trpc.content.createSeason.mutationOptions(),
  );
  const uploadAssetsMutation = useMutation(
    trpc.content.uploadAssets.mutationOptions(),
  );

  // ── AI flow ───────────────────────────────────────────────────────────────

  const startGeneration = async (match: ContentMatch) => {
    setSelectedMatch(match);
    setPhase("generating");
    setContentDetails(null);
    setContentImages(null);
    setImageGenFailed(false);

    const name = `${match.title} (${match.year})`;
    let details: ContentDetails | null = null;

    try {
      if (contentType === "MOVIE") {
        details = await movieDetailsMutation.mutateAsync({ movieName: name });
      } else {
        details = await contentDetailsMutation.mutateAsync({
          name,
          contentType: "SERIES",
        });
      }
    } catch {
      toast.error("Failed to fetch details. Please try again.");
      setPhase("search");
      return;
    }

    if (!details) {
      toast.error("Could not fetch details.");
      setPhase("search");
      return;
    }

    setContentDetails(details);

    try {
      if (contentType === "MOVIE") {
        const images = await movieImagesMutation.mutateAsync(details);
        setContentImages(images);
      } else {
        const images = await contentImagesMutation.mutateAsync(details);
        setContentImages(images);
      }
    } catch {
      toast.error(
        "Image generation was blocked by content policy. Please upload images manually.",
      );
      setImageGenFailed(true);
    }

    setPhase("review");
  };

  const handleSearch = async (title: string) => {
    try {
      const data =
        contentType === "MOVIE"
          ? await searchMovieMutation.mutateAsync({ title })
          : await searchContentMutation.mutateAsync({
              title,
              contentType: "SERIES",
            });
      if (!data) return;

      if (data.matches.length === 1) {
        await startGeneration(data.matches[0]);
      } else if (data.matches.length > 1) {
        setPhase("select");
      } else {
        const kind = contentType === "SERIES" ? "series" : "movies";
        toast.error(`No ${kind} found. Try a different name.`);
      }
    } catch {
      toast.error("Search failed. Please try again.");
    }
  };

  const handleSelectMatch = async (match: ContentMatch) => {
    await startGeneration(match);
  };

  // ── Manual image upload (fallback when AI generation fails) ───────────────

  const handleManualImages = (images: ContentImages) => {
    setContentImages(images);
  };

  // ── Content save flow (create + optional season + upload assets) ───────────

  const handleProceedToUpload = async () => {
    if (!contentDetails || !contentImages) return;

    setPhase("saving");

    try {
      const content = await createContentMutation.mutateAsync({
        title: contentDetails.title,
        description: contentDetails.description,
        contentType,
        releaseYear: contentDetails.release_year,
        rating: String(contentDetails.rating),
      });

      setContentId(content.id);

      if (contentType === "SERIES" && season != null && season > 0) {
        await createSeasonMutation.mutateAsync({
          contentId: content.id,
          seasonNumber: season,
          title: `${contentDetails.title} — Season ${season}`,
        });
      }

      await uploadAssetsMutation.mutateAsync({
        contentId: content.id,
        posterBase64: contentImages.poster,
        thumbnailBase64: contentImages.thumbnail,
      });

      setPhase("upload");
    } catch {
      toast.error("Failed to save content. Please try again.");
      setPhase("review");
    }
  };

  // ── Navigation ────────────────────────────────────────────────────────────

  const handleBackToReview = () => {
    setPhase("review");
  };

  const handleReset = () => {
    setPhase("search");
    setSeason(null);
    setSelectedMatch(null);
    setContentDetails(null);
    setContentImages(null);
    setContentId(null);
    setImageGenFailed(false);
    searchMovieMutation.reset();
    searchContentMutation.reset();
    movieDetailsMutation.reset();
    contentDetailsMutation.reset();
    movieImagesMutation.reset();
    contentImagesMutation.reset();
    createContentMutation.reset();
    createSeasonMutation.reset();
    uploadAssetsMutation.reset();
  };

  return {
    phase,
    contentType,
    season,
    setContentType,
    setSeason,
    contentId,
    selectedMatch,
    contentDetails,
    contentImages,
    imageGenFailed,
    searchResults:
      contentType === "SERIES"
        ? searchContentMutation.data
        : searchMovieMutation.data,

    isSearching:
      searchMovieMutation.isPending || searchContentMutation.isPending,
    isGeneratingDetails:
      movieDetailsMutation.isPending || contentDetailsMutation.isPending,
    isGeneratingImages:
      movieImagesMutation.isPending || contentImagesMutation.isPending,
    isGenerating:
      movieDetailsMutation.isPending ||
      contentDetailsMutation.isPending ||
      movieImagesMutation.isPending ||
      contentImagesMutation.isPending,

    isSaving: phase === "saving",
    isCreatingContent: createContentMutation.isPending,
    isUploadingAssets: uploadAssetsMutation.isPending,

    handleSearch,
    handleSelectMatch,
    handleManualImages,
    handleProceedToUpload,
    handleBackToReview,
    handleReset,
  };
}

/** @deprecated Use useContentGeneration */
export function useMovieGeneration() {
  const out = useContentGeneration();
  return {
    ...out,
    selectedMovie: out.selectedMatch,
    movieDetails: out.contentDetails,
    movieImages: out.contentImages,
    handleSelectMovie: out.handleSelectMatch,
  };
}
