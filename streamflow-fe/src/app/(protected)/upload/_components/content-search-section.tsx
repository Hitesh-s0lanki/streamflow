"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Film, Sparkles, Loader2, Tv } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import type { ContentTypeKind } from "./use-movie-generation";

interface ContentSearchSectionProps {
  contentType: ContentTypeKind;
  season: number | null;
  onContentTypeChange: (type: ContentTypeKind) => void;
  onSeasonChange: (season: number | null) => void;
  onSearch: (title: string) => void;
  isSearching: boolean;
}

export function ContentSearchSection({
  contentType,
  season,
  onContentTypeChange,
  onSeasonChange,
  onSearch,
  isSearching,
}: ContentSearchSectionProps) {
  const [title, setTitle] = useState("");
  const [seasonInput, setSeasonInput] = useState(season?.toString() ?? "");
  const [isFocused, setIsFocused] = useState(false);

  const isSeries = contentType === "SERIES";
  const label = isSeries ? "Series Name" : "Movie Name";
  const placeholder = isSeries ? "e.g. Breaking Bad" : "e.g. Interstellar";

  const isValid = title.trim().length > 0;
  const seasonNum =
    seasonInput.trim() === ""
      ? null
      : Math.max(1, parseInt(seasonInput, 10) || 1);

  const handleSubmit = () => {
    if (!isValid || isSearching) return;
    if (isSeries && seasonNum != null) {
      onSeasonChange(seasonNum);
    }
    onSearch(title.trim());
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.35 }}
      className={`rounded-md border p-6 transition-shadow duration-300 sm:p-8 ${
        isFocused ? "shadow-lg ring-1 ring-primary/20" : ""
      }`}
    >
      <div className="space-y-5">
        {/* Content type: Movie | Series */}
        <div className="space-y-2">
          <Label className="text-foreground text-sm font-medium">
            Content Type
          </Label>
          <div className="flex gap-2">
            <Button
              type="button"
              variant={contentType === "MOVIE" ? "default" : "outline"}
              size="sm"
              className="gap-1.5"
              onClick={() => onContentTypeChange("MOVIE")}
            >
              <Film className="size-3.5" />
              Movie
            </Button>
            <Button
              type="button"
              variant={contentType === "SERIES" ? "default" : "outline"}
              size="sm"
              className="gap-1.5"
              onClick={() => onContentTypeChange("SERIES")}
            >
              <Tv className="size-3.5" />
              Series
            </Button>
          </div>
        </div>

        {/* Season (only for Series) */}
        <AnimatePresence>
          {isSeries && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className="space-y-2"
            >
              <Label htmlFor="season" className="text-foreground text-sm font-medium">
                Season (optional)
              </Label>
              <Input
                id="season"
                type="number"
                min={1}
                placeholder="e.g. 1"
                value={seasonInput}
                onChange={(e) => setSeasonInput(e.target.value)}
                disabled={isSearching}
                className="h-11 w-28 text-sm"
              />
            </motion.div>
          )}
        </AnimatePresence>

        {/* Title */}
        <div className="space-y-2">
          <Label htmlFor="content-name" className="text-foreground text-sm font-medium">
            {label}
          </Label>
          <div className="relative">
            <Film className="text-muted-foreground pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2" />
            <Input
              id="content-name"
              type="text"
              placeholder={placeholder}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              onFocus={() => setIsFocused(true)}
              onBlur={() => setIsFocused(false)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleSubmit();
              }}
              disabled={isSearching}
              className="h-11 pl-10 text-sm"
            />
          </div>
          <motion.p
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6 }}
            className="text-muted-foreground mt-5 text-start text-xs"
          >
            Metadata, thumbnails, and streaming assets will be created
            automatically.
          </motion.p>
        </div>

        <AnimatePresence>
          {isValid && !isSearching && (
            <motion.p
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className="text-muted-foreground flex items-center gap-1.5 text-xs"
            >
              <span className="bg-primary inline-block size-1.5 rounded-full" />
              Ready to generate for{" "}
              <span className="text-foreground font-medium">
                {title.trim()}
                {isSeries && seasonNum != null ? ` — Season ${seasonNum}` : ""}
              </span>
            </motion.p>
          )}
        </AnimatePresence>

        <Button
          size="lg"
          disabled={!isValid || isSearching}
          onClick={handleSubmit}
          className="gap-2 text-sm font-semibold"
        >
          {isSearching ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Sparkles className="size-4" />
          )}
          {isSearching ? "Searching..." : "Generate"}
        </Button>
      </div>
    </motion.div>
  );
}
