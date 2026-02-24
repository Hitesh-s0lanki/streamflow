"use client";

import { motion } from "framer-motion";
import { Film, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import type {
  ContentMatch,
  ContentSearchResult,
  ContentTypeKind,
} from "./use-movie-generation";

interface ContentSelectionSectionProps {
  contentType: ContentTypeKind;
  searchResults: ContentSearchResult;
  onSelect: (match: ContentMatch) => void;
  onBack: () => void;
}

export function ContentSelectionSection({
  contentType,
  searchResults,
  onSelect,
  onBack,
}: ContentSelectionSectionProps) {
  const kind = contentType === "SERIES" ? "series" : "movie";

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="rounded-md border p-6 sm:p-8"
    >
      <div className="space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-foreground text-sm font-semibold">
              Multiple matches found
            </h3>
            <p className="text-muted-foreground mt-1 text-xs">
              Select the correct {kind} to continue
            </p>
          </div>
          <Button variant="ghost" size="sm" onClick={onBack} className="gap-1.5">
            <ArrowLeft className="size-3.5" />
            Back
          </Button>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          {searchResults.matches.map((match, index) => (
            <motion.button
              key={`${match.title}-${match.year}`}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              onClick={() => onSelect(match)}
              className="group flex items-center gap-3 rounded-lg border p-4 text-left transition-all hover:border-primary/40 hover:bg-primary/5 hover:shadow-sm"
            >
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-muted transition-colors group-hover:bg-primary/10">
                <Film className="size-4 text-muted-foreground transition-colors group-hover:text-primary" />
              </div>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-foreground">
                  {match.title}
                </p>
                <p className="text-xs text-muted-foreground">{match.year}</p>
              </div>
            </motion.button>
          ))}
        </div>

        {searchResults.suggestion && searchResults.suggestion.length > 0 && (
          <div className="rounded-lg bg-muted/50 p-3">
            <p className="text-xs font-medium text-muted-foreground">
              Did you mean:
            </p>
            <div className="mt-1.5 flex flex-wrap gap-1.5">
              {searchResults.suggestion.map((s) => (
                <span
                  key={s}
                  className="rounded-full bg-background px-2.5 py-0.5 text-xs text-foreground"
                >
                  {s}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </motion.div>
  );
}
