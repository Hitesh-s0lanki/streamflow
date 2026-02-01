"use client";

import Link from "next/link";
import { Play, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { ContentDetail } from "@/types/content";

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
  const backdropUrl = content.posterUrl ?? content.thumbnailUrl ?? "";
  const duration = formatDuration(content.durationSeconds ?? null);

  return (
    <div className="relative h-[80vh] md:h-[90vh] w-full overflow-hidden">
      <div className="absolute inset-0">
        {backdropUrl ? (
          <img
            src={backdropUrl}
            alt={content.title}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full bg-muted" />
        )}
        <div className="absolute inset-0 bg-gradient-to-r from-background via-background/60 to-transparent" />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-transparent to-transparent" />
      </div>

      <div className="absolute bottom-0 left-0 right-0 p-4 md:p-12 pb-24 md:pb-32">
        <div className="max-w-2xl animate-slide-up">
          <h1 className="text-4xl md:text-6xl font-bold text-foreground mb-4 leading-tight">
            {content.title}
          </h1>

          <div className="flex items-center gap-3 text-sm md:text-base text-muted-foreground mb-4">
            {content.rating && (
              <span className="text-primary font-semibold">{content.rating}</span>
            )}
            {content.releaseYear != null && <span>{content.releaseYear}</span>}
            {duration && (
              <>
                <span>•</span>
                <span>{duration}</span>
              </>
            )}
          </div>

          {content.description && (
            <p className="text-base md:text-lg text-foreground/80 mb-6 line-clamp-3 max-w-xl">
              {content.description}
            </p>
          )}

          <div className="flex items-center gap-4">
            <Button variant="hero" size="lg" asChild>
              <Link href={`/content/${content.id}`}>
                <Play className="h-5 w-5 fill-current" />
                Play
              </Link>
            </Button>
            <Button variant="heroSecondary" size="lg" asChild>
              <Link href={`/content/${content.id}`}>
                <Info className="h-5 w-5" />
                More Info
              </Link>
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
