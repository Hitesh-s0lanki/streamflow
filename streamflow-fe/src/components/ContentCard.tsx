"use client";

import Link from "next/link";
import { Play, Plus, Info } from "lucide-react";
import type { ContentCatalogItem, ContinueWatchingItem } from "@/types/content";

function formatDuration(seconds: number | null): string {
  if (seconds == null || seconds <= 0) return "";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

type CardItem = ContentCatalogItem | ContinueWatchingItem;

function isContinueItem(item: CardItem): item is ContinueWatchingItem {
  return "videoAssetId" in item && "lastWatchedSecond" in item;
}

interface ContentCardProps {
  item: CardItem;
  showProgress?: boolean;
}

export default function ContentCard({ item, showProgress = false }: ContentCardProps) {
  const isContinue = isContinueItem(item);
  const contentId = isContinue ? item.contentId : item.id;
  const title = item.title;
  const posterUrl = item.posterUrl ?? item.thumbnailUrl ?? "";
  const year = "releaseYear" in item ? item.releaseYear : null;
  const durationSeconds = "durationSeconds" in item ? item.durationSeconds : null;
  const duration = formatDuration(durationSeconds ?? null);
  const progress =
    showProgress && isContinue && item.durationSeconds
      ? Math.min(100, (item.lastWatchedSecond / item.durationSeconds) * 100)
      : null;

  return (
    <Link
      href={`/content/${contentId}`}
      className="content-card group shrink-0 w-[160px] md:w-[200px] block"
    >
      <div className="relative aspect-poster rounded-md overflow-hidden">
        {posterUrl ? (
          <img
            src={posterUrl}
            alt={title}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full bg-muted" />
        )}
        <div className="absolute inset-0 card-gradient opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
        {showProgress && progress != null && (
          <div className="absolute bottom-0 left-0 right-0 h-1 bg-muted">
            <div
              className="h-full bg-primary"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}
        <div className="absolute inset-0 flex flex-col justify-end p-3 opacity-0 group-hover:opacity-100 transition-all duration-300">
          <h3 className="font-semibold text-sm text-foreground mb-1 line-clamp-2">
            {title}
            {isContinue && item.episodeTitle ? ` · ${item.episodeTitle}` : ""}
          </h3>
          <div className="flex items-center gap-2 text-xs text-muted-foreground mb-2">
            {year != null && <span>{year}</span>}
            {duration && (
              <>
                {year != null && <span>•</span>}
                <span>{duration}</span>
              </>
            )}
          </div>
          <div className="flex items-center gap-2">
            {isContinue && (
              <Link
                href={`/play/${item.videoAssetId}`}
                className="h-8 w-8 rounded-full bg-foreground flex items-center justify-center hover:bg-foreground/80 transition-colors"
                onClick={(e) => e.stopPropagation()}
              >
                <Play className="h-4 w-4 text-background fill-current" />
              </Link>
            )}
            <button
              type="button"
              className="h-8 w-8 rounded-full border border-muted-foreground flex items-center justify-center hover:border-foreground transition-colors"
              onClick={(e) => e.preventDefault()}
            >
              <Plus className="h-4 w-4 text-foreground" />
            </button>
            <Link
              href={`/content/${contentId}`}
              className="h-8 w-8 rounded-full border border-muted-foreground flex items-center justify-center hover:border-foreground transition-colors"
              onClick={(e) => e.stopPropagation()}
            >
              <Info className="h-4 w-4 text-foreground" />
            </Link>
          </div>
        </div>
      </div>
    </Link>
  );
}
