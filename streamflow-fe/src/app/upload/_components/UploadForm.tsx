"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { useMutation } from "@tanstack/react-query";
import {
  Upload as UploadIcon,
  Film,
  Tv,
  X,
  Plus,
  ImageIcon,
  Video,
  CheckCircle2,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { cn } from "@/lib/utils";
import {
  createContent,
  createSeason,
  createEpisode,
  createVideoAsset,
  getUploadUrl,
  uploadFileToPresignedUrl,
  confirmUpload,
  getIngestionStatus,
  type ContentType,
  type CreateContentRequest,
  type IngestionStatus,
} from "@/lib/api/upload";
import { ApiError } from "@/lib/api/client";

const RATINGS = [
  { value: "", label: "Select rating" },
  { value: "G", label: "G" },
  { value: "PG", label: "PG" },
  { value: "PG-13", label: "PG-13" },
  { value: "R", label: "R" },
  { value: "TV-Y", label: "TV-Y" },
  { value: "TV-PG", label: "TV-PG" },
  { value: "TV-14", label: "TV-14" },
  { value: "TV-MA", label: "TV-MA" },
];

const POLL_INTERVAL_MS = 5000;
const MAX_POLL_ATTEMPTS = 60;

const UPLOAD_STEPS = [
  { label: "Creating content", keys: ["Creating content..."] },
  {
    label: "Setting up video",
    keys: [
      "Creating video asset...",
      "Creating season...",
      "Creating episode...",
    ],
  },
  { label: "Getting upload URL", keys: ["Getting upload URL..."] },
  { label: "Uploading video", keys: ["Uploading video..."] },
  { label: "Confirming upload", keys: ["Confirming upload..."] },
  { label: "Processing", keys: ["Processing..."] },
];

function getStepIndex(stepMessage: string | null): number {
  if (!stepMessage) return -1;
  const idx = UPLOAD_STEPS.findIndex((s) => s.keys.includes(stepMessage));
  return idx >= 0 ? idx : 0;
}

export function UploadForm() {
  const router = useRouter();
  const [contentType, setContentType] = useState<ContentType>("MOVIE");
  const [genres, setGenres] = useState<string[]>([]);
  const [newGenre, setNewGenre] = useState("");
  const [posterPreview, setPosterPreview] = useState<string | null>(null);
  const [backdropPreview, setBackdropPreview] = useState<string | null>(null);
  const [videoFile, setVideoFile] = useState<File | null>(null);
  const [stepMessage, setStepMessage] = useState<string | null>(null);
  const [ingestionStatus, setIngestionStatus] =
    useState<IngestionStatus | null>(null);

  const handlePosterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => setPosterPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleBackdropChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => setBackdropPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleVideoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) setVideoFile(file);
  };

  const addGenre = () => {
    if (newGenre.trim() && !genres.includes(newGenre.trim())) {
      setGenres([...genres, newGenre.trim()]);
      setNewGenre("");
    }
  };

  const removeGenre = (genreToRemove: string) => {
    setGenres(genres.filter((g) => g !== genreToRemove));
  };

  const runUploadFlow = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.currentTarget;
    const title = (
      form.elements.namedItem("title") as HTMLInputElement
    )?.value?.trim();
    const description =
      (
        form.elements.namedItem("description") as HTMLTextAreaElement
      )?.value?.trim() || null;
    const releaseYearStr = (
      form.elements.namedItem("year") as HTMLInputElement
    )?.value?.trim();
    const releaseYear = releaseYearStr ? parseInt(releaseYearStr, 10) : null;
    const rating =
      (form.elements.namedItem("rating") as HTMLSelectElement)?.value?.trim() ||
      null;
    const posterUrl =
      (
        form.elements.namedItem("posterUrl") as HTMLInputElement
      )?.value?.trim() || null;
    const thumbnailUrl =
      (
        form.elements.namedItem("thumbnailUrl") as HTMLInputElement
      )?.value?.trim() || null;

    if (!title) throw new Error("Title is required");

    const durationInput = (
      form.elements.namedItem("duration") as HTMLInputElement
    )?.value?.trim();
    const durationMinutes = durationInput ? parseInt(durationInput, 10) : 0;
    const durationSeconds = durationMinutes * 60;
    if (durationSeconds <= 0) throw new Error("Duration is required");

    if (!videoFile) throw new Error("Video file is required");

    setStepMessage("Creating content...");
    const contentPayload: CreateContentRequest = {
      title,
      contentType,
      description: description || undefined,
      releaseYear:
        releaseYear && !Number.isNaN(releaseYear) ? releaseYear : undefined,
      rating: rating || undefined,
      posterUrl: posterUrl || undefined,
      thumbnailUrl: thumbnailUrl || undefined,
    };
    const content = await createContent(contentPayload);
    const contentId = content.id;

    let videoAssetId: string;

    if (contentType === "MOVIE") {
      setStepMessage("Creating video asset...");
      const asset = await createVideoAsset({
        contentId,
        durationSeconds,
      });
      videoAssetId = asset.id;
    } else {
      setStepMessage("Creating season...");
      const season = await createSeason(contentId, {
        seasonNumber: 1,
        title: "Season 1",
      });
      const episodeTitle =
        (
          form.elements.namedItem("episodeTitle") as HTMLInputElement
        )?.value?.trim() || "Episode 1";
      setStepMessage("Creating episode...");
      const episode = await createEpisode(season.id, {
        episodeNumber: 1,
        title: episodeTitle,
        durationSeconds,
      });
      setStepMessage("Creating video asset...");
      const asset = await createVideoAsset({
        episodeId: episode.id,
        durationSeconds,
      });
      videoAssetId = asset.id;
    }

    setStepMessage("Getting upload URL...");
    const { uploadUrl, rawS3Key } = await getUploadUrl(videoAssetId);

    setStepMessage("Uploading video...");
    await uploadFileToPresignedUrl(uploadUrl, videoFile);

    setStepMessage("Confirming upload...");
    await confirmUpload(videoAssetId, {
      rawS3Key,
      contentType: videoFile.type || "video/mp4",
    });

    setStepMessage("Processing...");
    let attempts = 0;
    while (attempts < MAX_POLL_ATTEMPTS) {
      const statusRes = await getIngestionStatus(videoAssetId);
      setIngestionStatus(statusRes.jobStatus);
      if (statusRes.jobStatus === "READY") {
        return { success: true, contentId };
      }
      if (statusRes.jobStatus === "FAILED") {
        throw new Error(statusRes.errorMessage || "Processing failed");
      }
      await new Promise((r) => setTimeout(r, POLL_INTERVAL_MS));
      attempts++;
    }
    throw new Error("Processing timed out. Check ingestion status later.");
  };

  const mutation = useMutation({
    mutationFn: runUploadFlow,
    onSuccess: () => {
      setStepMessage(null);
      setIngestionStatus(null);
      router.push("/");
    },
    onError: () => {
      setStepMessage(null);
    },
  });

  const isSubmitting = mutation.isPending;
  const currentStepIndex = getStepIndex(stepMessage);
  const errorMessage =
    mutation.isError && mutation.error instanceof Error
      ? mutation.error.message
      : mutation.isError && mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.isError
      ? "Something went wrong. Please try again."
      : null;

  return (
    <form onSubmit={mutation.mutate} className="space-y-8">
      {errorMessage && (
        <Alert
          variant="destructive"
          className="rounded-xl border-destructive/50"
        >
          <AlertCircle className="size-4" />
          <AlertTitle>Upload failed</AlertTitle>
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      {/* Content type */}
      <Card className="rounded-xl border-border/60 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">
            Content type
          </CardTitle>
          <CardDescription>
            Choose whether you&apos;re uploading a movie or the first episode of
            a series.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => setContentType("MOVIE")}
              className={cn(
                "flex items-center gap-4 rounded-xl border-2 px-5 py-4 text-left transition-all duration-200 min-w-[140px]",
                contentType === "MOVIE"
                  ? "border-primary bg-primary/10 text-foreground shadow-sm"
                  : "border-border bg-muted/20 text-muted-foreground hover:border-muted-foreground/40 hover:bg-muted/30"
              )}
            >
              <span className="flex size-11 items-center justify-center rounded-lg bg-background/80 shadow-sm">
                <Film className="size-5 text-primary" />
              </span>
              <span className="font-medium">Movie</span>
            </button>
            <button
              type="button"
              onClick={() => setContentType("SERIES")}
              className={cn(
                "flex items-center gap-4 rounded-xl border-2 px-5 py-4 text-left transition-all duration-200 min-w-[140px]",
                contentType === "SERIES"
                  ? "border-primary bg-primary/10 text-foreground shadow-sm"
                  : "border-border bg-muted/20 text-muted-foreground hover:border-muted-foreground/40 hover:bg-muted/30"
              )}
            >
              <span className="flex size-11 items-center justify-center rounded-lg bg-background/80 shadow-sm">
                <Tv className="size-5 text-primary" />
              </span>
              <span className="font-medium">Series</span>
            </button>
          </div>
        </CardContent>
      </Card>

      {/* Basic information */}
      <Card className="rounded-xl border-border/60 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">
            Basic information
          </CardTitle>
          <CardDescription>Title, description, and metadata.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="space-y-2">
            <Label htmlFor="title" className="text-sm font-medium">
              Title <span className="text-destructive">*</span>
            </Label>
            <Input
              id="title"
              name="title"
              placeholder="e.g. The Great Adventure"
              required
              className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background transition-colors"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description" className="text-sm font-medium">
              Description
            </Label>
            <Textarea
              id="description"
              name="description"
              placeholder="Brief summary of the content..."
              rows={3}
              className="rounded-lg border-border bg-muted/20 focus:bg-background resize-none transition-colors"
            />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="year" className="text-sm font-medium">
                Release year
              </Label>
              <Input
                id="year"
                name="year"
                type="number"
                placeholder="2024"
                min={1900}
                max={2100}
                className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="duration" className="text-sm font-medium">
                {contentType === "MOVIE"
                  ? "Duration (minutes)"
                  : "Episode duration (minutes)"}{" "}
                <span className="text-destructive">*</span>
              </Label>
              <Input
                id="duration"
                name="duration"
                type="number"
                placeholder={contentType === "MOVIE" ? "120" : "45"}
                min={1}
                required
                className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background"
              />
            </div>
          </div>
          {contentType === "SERIES" && (
            <div className="space-y-2">
              <Label htmlFor="episodeTitle" className="text-sm font-medium">
                Episode title <span className="text-destructive">*</span>
              </Label>
              <Input
                id="episodeTitle"
                name="episodeTitle"
                placeholder="e.g. Episode 1"
                required
                className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background"
              />
            </div>
          )}
          <div className="space-y-2">
            <Label htmlFor="rating" className="text-sm font-medium">
              Rating
            </Label>
            <select
              id="rating"
              name="rating"
              className="h-10 w-full rounded-lg border border-border bg-muted/20 px-3 text-sm text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-colors"
            >
              {RATINGS.map((r) => (
                <option key={r.value || "empty"} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Artwork & media */}
      <Card className="rounded-xl border-border/60 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">
            Artwork & URLs
          </CardTitle>
          <CardDescription>
            Optional poster and backdrop URLs. You can also preview local images
            below.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="posterUrl" className="text-sm font-medium">
                Poster URL
              </Label>
              <Input
                id="posterUrl"
                name="posterUrl"
                type="url"
                placeholder="https://..."
                className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="thumbnailUrl" className="text-sm font-medium">
                Backdrop / thumbnail URL
              </Label>
              <Input
                id="thumbnailUrl"
                name="thumbnailUrl"
                type="url"
                placeholder="https://..."
                className="h-10 rounded-lg border-border bg-muted/20 focus:bg-background"
              />
            </div>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
            <div className="space-y-2">
              <span className="text-sm font-medium text-muted-foreground">
                Poster preview
              </span>
              <div
                className={cn(
                  "relative rounded-xl border-2 border-dashed overflow-hidden transition-colors",
                  posterPreview
                    ? "border-primary/50 aspect-2/3 max-h-[280px]"
                    : "aspect-2/3 max-h-[280px] border-border bg-muted/20 hover:border-muted-foreground/40 hover:bg-muted/30"
                )}
              >
                {posterPreview ? (
                  <>
                    <Image
                      src={posterPreview}
                      alt="Poster"
                      fill
                      className="object-cover"
                      unoptimized
                    />
                    <button
                      type="button"
                      onClick={() => setPosterPreview(null)}
                      className="absolute top-2 right-2 size-8 rounded-full bg-background/90 shadow-sm flex items-center justify-center hover:bg-background transition-colors"
                      aria-label="Remove poster"
                    >
                      <X className="size-4" />
                    </button>
                  </>
                ) : (
                  <label className="absolute inset-0 flex flex-col items-center justify-center cursor-pointer p-4 gap-2">
                    <ImageIcon className="size-8 text-muted-foreground" />
                    <span className="text-xs text-muted-foreground text-center">
                      Click to preview
                    </span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handlePosterChange}
                      className="hidden"
                    />
                  </label>
                )}
              </div>
            </div>
            <div className="space-y-2">
              <span className="text-sm font-medium text-muted-foreground">
                Backdrop preview
              </span>
              <div
                className={cn(
                  "relative rounded-xl border-2 border-dashed overflow-hidden transition-colors aspect-video",
                  backdropPreview
                    ? "border-primary/50"
                    : "border-border bg-muted/20 hover:border-muted-foreground/40 hover:bg-muted/30"
                )}
              >
                {backdropPreview ? (
                  <>
                    <Image
                      src={backdropPreview}
                      alt="Backdrop"
                      fill
                      className="object-cover"
                      unoptimized
                    />
                    <button
                      type="button"
                      onClick={() => setBackdropPreview(null)}
                      className="absolute top-2 right-2 size-8 rounded-full bg-background/90 shadow-sm flex items-center justify-center hover:bg-background transition-colors"
                      aria-label="Remove backdrop"
                    >
                      <X className="size-4" />
                    </button>
                  </>
                ) : (
                  <label className="absolute inset-0 flex flex-col items-center justify-center cursor-pointer p-4 gap-2">
                    <ImageIcon className="size-8 text-muted-foreground" />
                    <span className="text-xs text-muted-foreground text-center">
                      Click to preview
                    </span>
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleBackdropChange}
                      className="hidden"
                    />
                  </label>
                )}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Genres */}
      <Card className="rounded-xl border-border/60 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">Genres</CardTitle>
          <CardDescription>Add genres for display. Optional.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-2">
            <Input
              value={newGenre}
              onChange={(e) => setNewGenre(e.target.value)}
              placeholder="Add genre"
              className="h-9 w-full max-w-[180px] rounded-lg border-border bg-muted/20 text-sm"
              onKeyDown={(e) =>
                e.key === "Enter" && (e.preventDefault(), addGenre())
              }
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={addGenre}
              className="rounded-lg h-9 gap-1.5"
            >
              <Plus className="size-4" />
              Add
            </Button>
          </div>
          {genres.length > 0 && (
            <div className="flex flex-wrap gap-2 mt-3">
              {genres.map((genre) => (
                <span
                  key={genre}
                  className="inline-flex items-center gap-1.5 rounded-full bg-primary/15 text-primary px-3 py-1.5 text-sm font-medium"
                >
                  {genre}
                  <button
                    type="button"
                    onClick={() => removeGenre(genre)}
                    className="rounded-full p-0.5 hover:bg-primary/20 transition-colors"
                    aria-label={`Remove ${genre}`}
                  >
                    <X className="size-3.5" />
                  </button>
                </span>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Video file */}
      <Card className="rounded-xl border-border/60 shadow-sm">
        <CardHeader className="pb-4">
          <CardTitle className="text-base font-semibold">Video file</CardTitle>
          <CardDescription>
            Upload your video. MP4, MOV, or MKV. Required.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div
            className={cn(
              "relative rounded-xl border-2 border-dashed transition-all duration-200 p-8 min-h-[160px] flex items-center justify-center",
              videoFile
                ? "border-primary/50 bg-primary/5"
                : "border-border bg-muted/20 hover:border-muted-foreground/50 hover:bg-muted/30"
            )}
          >
            {videoFile ? (
              <div className="flex flex-wrap items-center justify-between gap-4 w-full">
                <div className="flex items-center gap-4">
                  <span className="flex size-12 items-center justify-center rounded-xl bg-primary/15">
                    <Video className="size-6 text-primary" />
                  </span>
                  <div>
                    <p className="font-medium text-foreground truncate max-w-[240px]">
                      {videoFile.name}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {(videoFile.size / (1024 * 1024)).toFixed(2)} MB
                    </p>
                  </div>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setVideoFile(null)}
                  className="rounded-lg gap-1.5"
                >
                  <X className="size-4" />
                  Remove
                </Button>
              </div>
            ) : (
              <label className="flex flex-col items-center justify-center cursor-pointer gap-3 text-center">
                <span className="flex size-14 items-center justify-center rounded-xl bg-muted">
                  <UploadIcon className="size-7 text-muted-foreground" />
                </span>
                <div>
                  <p className="text-sm font-medium text-foreground">
                    Click to upload video
                  </p>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    MP4, MOV, or MKV
                  </p>
                </div>
                <input
                  type="file"
                  accept="video/*"
                  onChange={handleVideoChange}
                  className="hidden"
                />
              </label>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Progress */}
      {isSubmitting && (stepMessage || ingestionStatus) && (
        <Card className="rounded-xl border-primary/30 bg-primary/5 shadow-sm">
          <CardHeader className="pb-3">
            <CardTitle className="text-base font-semibold flex items-center gap-2">
              <Loader2 className="size-4 animate-spin" />
              Upload in progress
            </CardTitle>
            <CardDescription>
              Do not close this page until processing completes.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="space-y-2">
              {UPLOAD_STEPS.map((step, i) => {
                const isActive = i === currentStepIndex;
                const isDone =
                  i < currentStepIndex ||
                  (i === 5 && ingestionStatus === "READY");
                return (
                  <li
                    key={step.label}
                    className={cn(
                      "flex items-center gap-3 text-sm",
                      isActive && "text-foreground font-medium",
                      isDone && "text-muted-foreground",
                      !isActive && !isDone && "text-muted-foreground/70"
                    )}
                  >
                    {isDone ? (
                      <CheckCircle2 className="size-4 shrink-0 text-primary" />
                    ) : isActive ? (
                      <Loader2 className="size-4 shrink-0 animate-spin text-primary" />
                    ) : (
                      <span className="size-4 shrink-0 rounded-full border-2 border-muted-foreground/30" />
                    )}
                    <span>{step.label}</span>
                    {isActive && ingestionStatus && i === 5 && (
                      <span className="text-muted-foreground font-normal">
                        — {ingestionStatus}
                      </span>
                    )}
                  </li>
                );
              })}
            </ul>
          </CardContent>
        </Card>
      )}

      {/* Footer actions */}
      <div className="flex flex-col-reverse sm:flex-row justify-end gap-3 pt-4 border-t border-border/60">
        <Button
          type="button"
          variant="outline"
          asChild
          className="rounded-lg px-6"
        >
          <Link href="/">Cancel</Link>
        </Button>
        <Button
          type="submit"
          variant="hero"
          disabled={isSubmitting}
          className="rounded-lg px-8 gap-2 shadow-lg shadow-primary/20"
        >
          {isSubmitting ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              Uploading...
            </>
          ) : (
            <>
              <UploadIcon className="size-4" />
              Upload content
            </>
          )}
        </Button>
      </div>
    </form>
  );
}
