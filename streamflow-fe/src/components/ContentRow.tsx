"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { useRef, useState } from "react";
import ContentCard from "@/components/ContentCard";
import type { ContentCatalogItem, ContinueWatchingItem } from "@/types/content";

type RowItem = ContentCatalogItem | ContinueWatchingItem;

interface ContentRowProps {
  title: string;
  items: RowItem[];
  showProgress?: boolean;
}

export default function ContentRow({
  title,
  items,
  showProgress = false,
}: ContentRowProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const [showLeftArrow, setShowLeftArrow] = useState(false);
  const [showRightArrow, setShowRightArrow] = useState(true);

  const scroll = (direction: "left" | "right") => {
    if (!scrollRef.current) return;
    const scrollAmount = scrollRef.current.clientWidth * 0.8;
    const newPosition =
      direction === "left"
        ? scrollRef.current.scrollLeft - scrollAmount
        : scrollRef.current.scrollLeft + scrollAmount;
    scrollRef.current.scrollTo({ left: newPosition, behavior: "smooth" });
  };

  const handleScroll = () => {
    if (!scrollRef.current) return;
    const { scrollLeft, scrollWidth, clientWidth } = scrollRef.current;
    setShowLeftArrow(scrollLeft > 0);
    setShowRightArrow(scrollLeft < scrollWidth - clientWidth - 10);
  };

  if (items.length === 0) return null;

  return (
    <div className="relative group/row py-4">
      <h2 className="text-xl md:text-2xl font-semibold text-foreground mb-4 px-4 md:px-12">
        {title}
      </h2>
      <div className="relative">
        {showLeftArrow && (
          <button
            type="button"
            onClick={() => scroll("left")}
            className="absolute left-0 top-0 bottom-0 z-10 w-12 bg-gradient-to-r from-background to-transparent opacity-0 group-hover/row:opacity-100 transition-opacity duration-300 flex items-center justify-start pl-2"
          >
            <div className="h-10 w-10 rounded-full bg-muted/80 flex items-center justify-center hover:bg-muted transition-colors">
              <ChevronLeft className="h-6 w-6 text-foreground" />
            </div>
          </button>
        )}
        <div
          ref={scrollRef}
          onScroll={handleScroll}
          className="flex gap-3 overflow-x-auto hide-scrollbar px-4 md:px-12 pb-4"
        >
          {items.map((item) => (
            <ContentCard
              key={
                "videoAssetId" in item
                  ? item.videoAssetId
                  : (item as ContentCatalogItem).id
              }
              item={item}
              showProgress={showProgress}
            />
          ))}
        </div>
        {showRightArrow && (
          <button
            type="button"
            onClick={() => scroll("right")}
            className="absolute right-0 top-0 bottom-0 z-10 w-12 bg-gradient-to-l from-background to-transparent opacity-0 group-hover/row:opacity-100 transition-opacity duration-300 flex items-center justify-end pr-2"
          >
            <div className="h-10 w-10 rounded-full bg-muted/80 flex items-center justify-center hover:bg-muted transition-colors">
              <ChevronRight className="h-6 w-6 text-foreground" />
            </div>
          </button>
        )}
      </div>
    </div>
  );
}
