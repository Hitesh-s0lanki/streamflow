"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Film, Sparkles, Loader2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

interface MovieSearchSectionProps {
  onSearch: (title: string) => void;
  isSearching: boolean;
}

export function MovieSearchSection({
  onSearch,
  isSearching,
}: MovieSearchSectionProps) {
  const [movieName, setMovieName] = useState("");
  const [isFocused, setIsFocused] = useState(false);

  const isValid = movieName.trim().length > 0;

  const handleSubmit = () => {
    if (!isValid || isSearching) return;
    onSearch(movieName.trim());
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
        <div className="space-y-2">
          <Label
            htmlFor="movie-name"
            className="text-foreground text-sm font-medium"
          >
            Movie Name
          </Label>
          <div className="relative">
            <Film className="text-muted-foreground pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2" />
            <Input
              id="movie-name"
              type="text"
              placeholder="e.g. Interstellar"
              value={movieName}
              onChange={(e) => setMovieName(e.target.value)}
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
                {movieName.trim()}
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
