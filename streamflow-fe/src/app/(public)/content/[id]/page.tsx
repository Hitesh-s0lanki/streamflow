"use client";

import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { ArrowLeft, Play, Calendar, Clock, Star, Film, Tv } from "lucide-react";
import { getContentDetail } from "@/lib/api/content";
import { useMediaUrl } from "@/hooks/use-media-url";
import { usePlaybackSession } from "@/hooks/use-playback-session";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import Navbar from "@/components/Navbar";
import type { ContentDetail } from "@/types/content";

function formatDuration(seconds: number | null | undefined): string {
  if (seconds == null || seconds <= 0) return "";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

function DetailSkeleton() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="relative h-[50vh] md:h-[60vh] w-full overflow-hidden">
        <Skeleton className="absolute inset-0 w-full h-full rounded-none" />
      </div>
      <div className="max-w-5xl mx-auto px-4 md:px-8 -mt-32 relative z-10 pb-16">
        <div className="flex flex-col md:flex-row gap-8">
          <Skeleton className="w-[200px] h-[300px] rounded-lg shrink-0" />
          <div className="flex-1 space-y-4 pt-4">
            <Skeleton className="h-10 w-3/4" />
            <div className="flex gap-3">
              <Skeleton className="h-6 w-16" />
              <Skeleton className="h-6 w-20" />
              <Skeleton className="h-6 w-16" />
            </div>
            <Skeleton className="h-24 w-full" />
            <Skeleton className="h-10 w-32" />
          </div>
        </div>
      </div>
    </div>
  );
}

function ErrorState({
  message,
  onRetry,
}: {
  message: string;
  onRetry: () => void;
}) {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="flex-1 flex flex-col items-center justify-center p-8 text-center min-h-[60vh]">
        <p className="text-destructive font-medium mb-2">
          Failed to load content
        </p>
        <p className="text-sm text-muted-foreground mb-4 max-w-md">{message}</p>
        <div className="flex gap-3">
          <Button variant="outline" asChild>
            <Link href="/">
              <ArrowLeft className="h-4 w-4" />
              Back to Home
            </Link>
          </Button>
          <Button onClick={onRetry}>Retry</Button>
        </div>
      </div>
    </div>
  );
}

function ContentMeta({ content }: { content: ContentDetail }) {
  const duration = formatDuration(content.durationSeconds);

  return (
    <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
      {content.contentType === "MOVIE" ? (
        <span className="flex items-center gap-1.5">
          <Film className="h-4 w-4" />
          Movie
        </span>
      ) : (
        <span className="flex items-center gap-1.5">
          <Tv className="h-4 w-4" />
          Series
        </span>
      )}

      {content.releaseYear != null && (
        <>
          <Separator orientation="vertical" className="h-4" />
          <span className="flex items-center gap-1.5">
            <Calendar className="h-4 w-4" />
            {content.releaseYear}
          </span>
        </>
      )}

      {content.rating && (
        <>
          <Separator orientation="vertical" className="h-4" />
          <span className="flex items-center gap-1.5">
            <Star className="h-4 w-4" />
            {content.rating}
          </span>
        </>
      )}

      {duration && (
        <>
          <Separator orientation="vertical" className="h-4" />
          <span className="flex items-center gap-1.5">
            <Clock className="h-4 w-4" />
            {duration}
          </span>
        </>
      )}
    </div>
  );
}

export default function ContentDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const contentId = params.id;

  const { data: content, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["content", "detail", contentId],
    queryFn: () => getContentDetail(contentId),
    enabled: !!contentId,
  });

  const mediaKey = content?.posterUrl ?? content?.thumbnailUrl;
  const backdropUrl = useMediaUrl(mediaKey);
  const posterUrl = useMediaUrl(mediaKey);

  // Prefetch playback session so "Play" → watch page is instant (cache hit).
  usePlaybackSession(contentId);

  if (isLoading) return <DetailSkeleton />;

  if (isError) {
    const message =
      error instanceof Error ? error.message : "Content not found.";
    return <ErrorState message={message} onRetry={() => refetch()} />;
  }

  if (!content) {
    return (
      <ErrorState message="Content not found." onRetry={() => refetch()} />
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {/* Backdrop */}
      <div className="relative h-[50vh] md:h-[60vh] w-full overflow-hidden -mt-[72px]">
        {backdropUrl ? (
          <img
            src={backdropUrl}
            alt={content.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full bg-muted" />
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-r from-background/80 via-transparent to-transparent" />
      </div>

      {/* Content area */}
      <div className="max-w-5xl mx-auto px-4 md:px-8 -mt-40 relative z-10 pb-16">
        <div className="flex flex-col md:flex-row gap-8">
          {/* Poster */}
          <div className="shrink-0 hidden md:block">
            <div className="w-[220px] aspect-poster rounded-lg overflow-hidden shadow-2xl ring-1 ring-white/10">
              {posterUrl ? (
                <img
                  src={posterUrl}
                  alt={content.title}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full bg-muted flex items-center justify-center">
                  <Film className="h-12 w-12 text-muted-foreground" />
                </div>
              )}
            </div>
          </div>

          {/* Details */}
          <div className="flex-1 min-w-0 pt-2 md:pt-8">
            <div className="flex items-center gap-3 mb-3">
              <Badge
                variant={
                  content.publishStatus === "PUBLISHED"
                    ? "default"
                    : "secondary"
                }
              >
                {content.publishStatus}
              </Badge>
            </div>

            <h1 className="text-3xl md:text-5xl font-bold text-foreground mb-4 leading-tight">
              {content.title}
            </h1>

            <ContentMeta content={content} />

            {content.description && (
              <p className="mt-6 text-base md:text-lg text-foreground/80 leading-relaxed max-w-2xl">
                {content.description}
              </p>
            )}

            <div className="flex items-center gap-4 mt-8">
              <Button variant="hero" size="lg" asChild>
                <Link href={`/watch/${content.id}`}>
                  <Play className="h-5 w-5 fill-current" />
                  Play
                </Link>
              </Button>
              <Button
                variant="heroSecondary"
                size="lg"
                onClick={() => router.back()}
              >
                <ArrowLeft className="h-5 w-5" />
                Go Back
              </Button>
            </div>

            <Separator className="my-8" />

            {/* Additional info */}
            <div className="grid grid-cols-2 md:grid-cols-3 gap-6 text-sm">
              <div>
                <p className="text-muted-foreground mb-1">Content Type</p>
                <p className="text-foreground font-medium">
                  {content.contentType === "MOVIE" ? "Movie" : "Series"}
                </p>
              </div>
              {content.releaseYear != null && (
                <div>
                  <p className="text-muted-foreground mb-1">Release Year</p>
                  <p className="text-foreground font-medium">
                    {content.releaseYear}
                  </p>
                </div>
              )}
              {content.rating && (
                <div>
                  <p className="text-muted-foreground mb-1">Rating</p>
                  <p className="text-foreground font-medium">
                    {content.rating}
                  </p>
                </div>
              )}
              {content.durationSeconds != null && (
                <div>
                  <p className="text-muted-foreground mb-1">Duration</p>
                  <p className="text-foreground font-medium">
                    {formatDuration(content.durationSeconds)}
                  </p>
                </div>
              )}
              <div>
                <p className="text-muted-foreground mb-1">Added</p>
                <p className="text-foreground font-medium">
                  {formatDate(content.createdAt)}
                </p>
              </div>
              {content.updatedAt !== content.createdAt && (
                <div>
                  <p className="text-muted-foreground mb-1">Last Updated</p>
                  <p className="text-foreground font-medium">
                    {formatDate(content.updatedAt)}
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
