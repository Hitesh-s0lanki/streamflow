"use client";

import { useRouter } from "next/navigation";
import { Play, Plus, Info } from "lucide-react";
import type { ContentCatalogItem, ContinueWatchingItem } from "@/types/content";
import { useMediaUrl } from "@/hooks/use-media-url";

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

export default function ContentCard({
  item,
  showProgress = false,
}: ContentCardProps) {
  const router = useRouter();
  const isContinue = isContinueItem(item);
  const contentId = isContinue ? item.contentId : item.id;
  const title = item.title;
  const imageKey = isContinue
    ? (item.posterUrl ?? item.thumbnailUrl)
    : item.thumbnailUrl;
  const resolvedImageUrl = useMediaUrl(imageKey);
  const year = "releaseYear" in item ? item.releaseYear : null;
  const durationSeconds =
    "durationSeconds" in item ? item.durationSeconds : null;
  const duration = formatDuration(durationSeconds ?? null);
  const progress =
    showProgress && isContinue && item.durationSeconds
      ? Math.min(100, (item.lastWatchedSecond / item.durationSeconds) * 100)
      : null;

  return (
    <div
      role="link"
      tabIndex={0}
      onClick={() => router.push(`/content/${contentId}`)}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          router.push(`/content/${contentId}`);
        }
      }}
      className="content-card group shrink-0 w-[150px] md:w-[185px] cursor-pointer"
    >
      {/* Poster */}
      <div className="relative aspect-2/3 rounded-xl overflow-hidden bg-muted ring-1 ring-black/5 transition-all duration-300 group-hover:shadow-xl group-hover:shadow-black/10 group-hover:-translate-y-1 group-hover:ring-black/10">
        {resolvedImageUrl ? (
          <img
            src={resolvedImageUrl}
            alt={title}
            className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full bg-linear-to-br from-muted to-muted-foreground/10 flex items-center justify-center">
            <span className="text-3xl font-bold text-muted-foreground/25">
              {title.charAt(0)}
            </span>
          </div>
        )}

        {/* Hover overlay */}
        <div className="absolute inset-0 bg-linear-to-t from-black/70 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />

        {/* Progress bar */}
        {showProgress && progress != null && (
          <div className="absolute bottom-0 left-0 right-0 h-1 bg-black/20 z-10">
            <div
              className="h-full bg-primary rounded-full"
              style={{ width: `${progress}%` }}
            />
          </div>
        )}

        {/* Hover actions */}
        <div className="absolute bottom-0 left-0 right-0 p-3 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-y-2 group-hover:translate-y-0">
          <div className="flex items-center gap-1.5">
            {isContinue && (
              <button
                type="button"
                className="h-8 w-8 rounded-full bg-white flex items-center justify-center hover:bg-white/90 transition-colors shadow-md"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  router.push(`/play/${item.videoAssetId}`);
                }}
              >
                <Play className="h-3.5 w-3.5 text-black fill-current" />
              </button>
            )}
            <button
              type="button"
              className="h-8 w-8 rounded-full border border-white/50 bg-black/30 backdrop-blur-sm flex items-center justify-center hover:bg-black/50 transition-colors"
              onClick={(e) => e.preventDefault()}
            >
              <Plus className="h-3.5 w-3.5 text-white" />
            </button>
            <button
              type="button"
              className="h-8 w-8 rounded-full border border-white/50 bg-black/30 backdrop-blur-sm flex items-center justify-center hover:bg-black/50 transition-colors"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                router.push(`/content/${contentId}`);
              }}
            >
              <Info className="h-3.5 w-3.5 text-white" />
            </button>
          </div>
        </div>
      </div>

      {/* Title & meta */}
      <div className="mt-2.5 px-0.5">
        <h3 className="text-sm font-medium text-foreground line-clamp-1 leading-snug">
          {title}
          {isContinue && item.episodeTitle ? ` · ${item.episodeTitle}` : ""}
        </h3>
        <div className="flex items-center gap-1.5 text-xs text-muted-foreground mt-0.5">
          {year != null && <span>{year}</span>}
          {year != null && duration && <span>·</span>}
          {duration && <span>{duration}</span>}
        </div>
      </div>
    </div>
  );
}
