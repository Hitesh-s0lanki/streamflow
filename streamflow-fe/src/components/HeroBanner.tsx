"use client";

import Image from "next/image";
import Link from "next/link";
import { Play, Info, Film, Tv } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ContentDetail } from "@/types/content";
import { useMediaUrl } from "@/hooks/use-media-url";

function formatDuration(seconds: number | null): string {
  if (seconds == null || seconds <= 0) return "";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

interface HeroBannerProps {
  content: ContentDetail;
}

export default function HeroBanner({ content }: HeroBannerProps) {
  const backdropKey = content.posterUrl ?? content.thumbnailUrl;
  const backdropUrl = useMediaUrl(backdropKey);
  const duration = formatDuration(content.durationSeconds ?? null);

  return (
    <section className="relative min-h-[50vh] h-[70vh] sm:min-h-[60vh] md:h-[80vh] w-full overflow-hidden -mt-14 sm:-mt-[4.5rem] md:-mt-[72px]">
      {/* Backdrop */}
      <div className="absolute inset-0">
        {backdropUrl ? (
          <Image
            src={backdropUrl}
            alt={content.title}
            fill
            className="object-cover"
            sizes="100vw"
          />
        ) : (
          <div className="w-full h-full bg-linear-to-br from-muted to-muted/60" />
        )}

        {/* Light-theme gradient overlays */}
        <div className="absolute inset-x-0 top-0 h-28 bg-linear-to-b from-white/90 to-transparent" />
        <div className="absolute inset-0 bg-linear-to-r from-white via-white/75 to-transparent" />
        <div className="absolute inset-0 bg-linear-to-t from-white via-white/30 to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 h-48 bg-linear-to-t from-background to-transparent" />
      </div>

      {/* Content */}
      <div className="absolute bottom-0 left-0 right-0 p-4 sm:p-6 md:p-16 pb-20 sm:pb-28 md:pb-36">
        <div className="max-w-2xl animate-slide-up">
          <div className="flex items-center gap-2 mb-4">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-semibold uppercase tracking-wider backdrop-blur-sm">
              {content.contentType === "MOVIE" ? (
                <>
                  <Film className="h-3 w-3" />
                  Movie
                </>
              ) : (
                <>
                  <Tv className="h-3 w-3" />
                  Series
                </>
              )}
            </span>
          </div>

          <h1 className="text-2xl min-[400px]:text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-bold text-foreground mb-3 leading-tight tracking-tight">
            {content.title}
          </h1>

          <div className="flex items-center gap-3 text-sm text-muted-foreground mb-4">
            {content.rating && (
              <span className="px-2 py-0.5 rounded border border-muted-foreground/30 text-xs font-medium">
                {content.rating}
              </span>
            )}
            {content.releaseYear != null && <span>{content.releaseYear}</span>}
            {duration && (
              <>
                <span className="text-muted-foreground/40">|</span>
                <span>{duration}</span>
              </>
            )}
          </div>

          {content.description && (
            <p className="text-base md:text-lg text-foreground/70 mb-8 line-clamp-3 max-w-xl leading-relaxed">
              {content.description}
            </p>
          )}

          <div className="flex flex-wrap items-center gap-2 sm:gap-3">
            <Button
              variant="hero"
              size="lg"
              className="rounded-full px-5 py-2.5 text-sm sm:px-8 sm:py-3 sm:text-base shadow-lg shadow-primary/25"
              asChild
            >
              <Link href={`/content/${content.id}`}>
                <Play className="h-4 w-4 sm:h-5 sm:w-5 fill-current" />
                Play Now
              </Link>
            </Button>
            <Button
              variant="heroSecondary"
              size="lg"
              className="rounded-full px-5 py-2.5 text-sm sm:px-8 sm:py-3 sm:text-base"
              asChild
            >
              <Link href={`/content/${content.id}`}>
                <Info className="h-4 w-4 sm:h-5 sm:w-5" />
                More Info
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </section>
  );
}
