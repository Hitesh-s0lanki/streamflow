"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { motion } from "framer-motion";
import {
  Loader2,
  CheckCircle2,
  Star,
  Calendar,
  ArrowLeft,
  ArrowRight,
  ImageIcon,
  Save,
  Upload,
  AlertTriangle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import Image from "next/image";
import type {
  MovieDetails,
  MovieImages,
  MovieMatch,
} from "./use-movie-generation";

// ── Props ────────────────────────────────────────────────────────────────────

interface GeneratedContentSectionProps {
  selectedMovie: MovieMatch;
  movieDetails: MovieDetails | null;
  movieImages: MovieImages | null;
  isGeneratingDetails: boolean;
  isGeneratingImages: boolean;
  isSaving: boolean;
  isCreatingContent: boolean;
  isUploadingAssets: boolean;
  onProceed: () => void;
  onBack: () => void;
  readOnly?: boolean;
  imageGenFailed?: boolean;
  onManualImagesUpload?: (images: MovieImages) => void;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function fileToDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// ── Sub-components ───────────────────────────────────────────────────────────

function StepIndicator({
  label,
  isLoading,
  isComplete,
}: {
  label: string;
  isLoading: boolean;
  isComplete: boolean;
}) {
  return (
    <div className="flex items-center gap-2 text-sm">
      {isLoading && <Loader2 className="size-4 animate-spin text-primary" />}
      {isComplete && <CheckCircle2 className="size-4 text-green-500" />}
      {!isLoading && !isComplete && (
        <div className="size-4 rounded-full border-2 border-muted" />
      )}
      <span
        className={
          isComplete
            ? "text-foreground font-medium"
            : isLoading
              ? "text-primary font-medium"
              : "text-muted-foreground"
        }
      >
        {label}
      </span>
    </div>
  );
}

function ImageUploadInput({
  label,
  preview,
  onFileSelect,
}: {
  label: string;
  preview: string | null;
  onFileSelect: (file: File) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <div className="flex flex-col gap-2">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      {preview ? (
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          className="relative h-56 w-full overflow-hidden rounded-lg border"
        >
          <Image src={preview} alt={label} fill className="object-cover" />
        </motion.div>
      ) : (
        <div
          onClick={() => inputRef.current?.click()}
          className="flex h-56 w-full cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed border-muted-foreground/25 transition-colors hover:border-primary/40 hover:bg-muted/50"
        >
          <Upload className="size-5 text-muted-foreground" />
          <p className="mt-2 text-xs text-muted-foreground">Click to upload</p>
          <p className="mt-1 text-[10px] text-muted-foreground/60">
            PNG, JPG, WebP
          </p>
        </div>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/png,image/jpeg,image/webp"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) onFileSelect(file);
        }}
        className="hidden"
      />
    </div>
  );
}

// ── Main Component ───────────────────────────────────────────────────────────

export function GeneratedContentSection({
  selectedMovie,
  movieDetails,
  movieImages,
  isGeneratingDetails,
  isGeneratingImages,
  isSaving,
  isCreatingContent,
  isUploadingAssets,
  onProceed,
  onBack,
  readOnly,
  imageGenFailed,
  onManualImagesUpload,
}: GeneratedContentSectionProps) {
  const isGenerating = isGeneratingDetails || isGeneratingImages;

  // ── Manual image upload state ─────────────────────────────────────────────

  const [posterDataUrl, setPosterDataUrl] = useState<string | null>(null);
  const [thumbnailDataUrl, setThumbnailDataUrl] = useState<string | null>(null);
  const manualUploadFired = useRef(false);

  const handlePosterFile = useCallback(async (file: File) => {
    const dataUrl = await fileToDataUrl(file);
    setPosterDataUrl(dataUrl);
  }, []);

  const handleThumbnailFile = useCallback(async (file: File) => {
    const dataUrl = await fileToDataUrl(file);
    setThumbnailDataUrl(dataUrl);
  }, []);

  useEffect(() => {
    if (
      manualUploadFired.current ||
      !posterDataUrl ||
      !thumbnailDataUrl ||
      !onManualImagesUpload
    )
      return;
    manualUploadFired.current = true;
    onManualImagesUpload({ poster: posterDataUrl, thumbnail: thumbnailDataUrl });
  }, [posterDataUrl, thumbnailDataUrl, onManualImagesUpload]);

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6"
    >
      {/* Generation Progress */}
      {isGenerating && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div>
              <h3 className="text-foreground text-sm font-semibold">
                Generating content for{" "}
                <span className="text-primary">{selectedMovie.title}</span>
              </h3>
              <p className="text-muted-foreground mt-1 text-xs">
                This may take a moment...
              </p>
            </div>

            <div className="space-y-3">
              <StepIndicator
                label="Fetching metadata"
                isLoading={isGeneratingDetails}
                isComplete={!!movieDetails}
              />
              <StepIndicator
                label="Generating poster & thumbnail"
                isLoading={isGeneratingImages}
                isComplete={!!movieImages}
              />
            </div>
          </div>
        </motion.div>
      )}

      {/* Movie Details Card */}
      {movieDetails && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-foreground text-sm font-semibold">
                Content Details
              </h3>
              {!isGenerating && !isSaving && (
                <Badge variant="secondary" className="gap-1 text-xs">
                  <CheckCircle2 className="size-3" />
                  Generated
                </Badge>
              )}
            </div>

            <div className="space-y-3">
              <div>
                <h4 className="text-lg font-bold text-foreground">
                  {movieDetails.title}
                </h4>
                <div className="mt-1.5 flex items-center gap-3">
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Calendar className="size-3" />
                    {movieDetails.release_year}
                  </span>
                  <span className="flex items-center gap-1 text-xs text-muted-foreground">
                    <Star className="size-3 fill-yellow-500 text-yellow-500" />
                    {movieDetails.rating}/10
                  </span>
                </div>
              </div>

              <p className="text-sm leading-relaxed text-muted-foreground">
                {movieDetails.description}
              </p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Manual Image Upload — shown when AI generation failed and no images yet */}
      {imageGenFailed && !movieImages && movieDetails && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div>
              <div className="flex items-center gap-2">
                <AlertTriangle className="size-4 text-yellow-500" />
                <h3 className="text-foreground text-sm font-semibold">
                  Upload Images Manually
                </h3>
              </div>
              <p className="mt-1 text-xs text-muted-foreground">
                AI image generation was blocked by content policy. Please upload
                a poster and thumbnail for this content.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-[160px_1fr]">
              <ImageUploadInput
                label="Poster"
                preview={posterDataUrl}
                onFileSelect={handlePosterFile}
              />
              <ImageUploadInput
                label="Thumbnail"
                preview={thumbnailDataUrl}
                onFileSelect={handleThumbnailFile}
              />
            </div>

            {posterDataUrl && !thumbnailDataUrl && (
              <p className="text-xs text-muted-foreground">
                Now upload a thumbnail to continue.
              </p>
            )}
            {!posterDataUrl && thumbnailDataUrl && (
              <p className="text-xs text-muted-foreground">
                Now upload a poster to continue.
              </p>
            )}
          </div>
        </motion.div>
      )}

      {/* Generated / Uploaded Images Preview */}
      {(isGeneratingImages || movieImages) && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <ImageIcon className="size-4 text-muted-foreground" />
              <h3 className="text-foreground text-sm font-semibold">
                {imageGenFailed ? "Uploaded Images" : "Generated Images"}
              </h3>
            </div>

            <div className="grid gap-4 sm:grid-cols-[160px_1fr]">
              {/* Poster */}
              <div className="flex flex-col gap-2">
                <p className="text-xs font-medium text-muted-foreground">
                  Poster
                </p>
                {movieImages?.poster ? (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="relative h-56 w-full overflow-hidden rounded-lg border"
                  >
                    <Image
                      src={movieImages.poster}
                      alt="Movie poster"
                      fill
                      className="object-cover"
                    />
                  </motion.div>
                ) : (
                  <Skeleton className="h-56 w-full rounded-lg" />
                )}
              </div>

              {/* Thumbnail */}
              <div className="flex w-full flex-col gap-2">
                <p className="text-xs font-medium text-muted-foreground">
                  Thumbnail
                </p>
                {movieImages?.thumbnail ? (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="relative h-56 overflow-hidden rounded-lg border"
                  >
                    <Image
                      src={movieImages.thumbnail}
                      alt="Movie thumbnail"
                      fill
                      className="object-cover"
                    />
                  </motion.div>
                ) : (
                  <Skeleton className="h-56 w-full rounded-lg" />
                )}
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* Saving Progress */}
      {isSaving && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="rounded-md border p-6 sm:p-8"
        >
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <Save className="size-4 text-muted-foreground" />
              <h3 className="text-foreground text-sm font-semibold">
                Saving content...
              </h3>
            </div>

            <div className="space-y-3">
              <StepIndicator
                label="Creating content entry"
                isLoading={isCreatingContent}
                isComplete={!isCreatingContent && isUploadingAssets}
              />
              <StepIndicator
                label="Uploading poster & thumbnail"
                isLoading={isUploadingAssets}
                isComplete={false}
              />
            </div>
          </div>
        </motion.div>
      )}

      {/* Actions */}
      {!isGenerating && !isSaving && !readOnly && movieDetails && movieImages && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="flex items-center justify-between"
        >
          <Button
            variant="ghost"
            size="sm"
            onClick={onBack}
            className="gap-1.5"
          >
            <ArrowLeft className="size-3.5" />
            Start Over
          </Button>
          <Button
            size="lg"
            onClick={onProceed}
            className="gap-2 text-sm font-semibold"
          >
            Proceed to Upload
            <ArrowRight className="size-4" />
          </Button>
        </motion.div>
      )}
    </motion.div>
  );
}
