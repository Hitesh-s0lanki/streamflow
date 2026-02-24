"use client";

import { useRef, useState, useCallback, useMemo } from "react";
import Image from "next/image";
import { motion, AnimatePresence } from "framer-motion";

const SPRITE_PATH = "/sprite.jpg";

/** Matches backend sprite sheet metadata (columns, rows, frame count, thumb dimensions). */
export type SpriteMetadata = {
  columns: number;
  rows: number;
  frames_count: number;
  thumb_width: number;
  thumb_height: number;
};

const DEFAULT_SPRITE: SpriteMetadata = {
  columns: 10,
  rows: 10,
  frames_count: 100,
  thumb_width: 160,
  thumb_height: 90,
};

function useSpriteFrame(percent: number | null, meta: SpriteMetadata) {
  return useMemo(() => {
    if (percent === null) return null;
    const { columns, rows, frames_count } = meta;
    const frameIndex = Math.min(
      frames_count - 1,
      Math.max(0, Math.floor((percent / 100) * frames_count)),
    );
    const col = frameIndex % columns;
    const row = Math.floor(frameIndex / columns);
    return { frameIndex, col, row, columns, rows };
  }, [percent, meta]);
}

export function SpriteSheetDemo({
  sprite = DEFAULT_SPRITE,
}: {
  sprite?: SpriteMetadata;
}) {
  const timelineRef = useRef<HTMLDivElement>(null);
  const scrubAreaRef = useRef<HTMLDivElement>(null);
  const [scrubPercent, setScrubPercent] = useState<number | null>(null);
  const [isHovering, setIsHovering] = useState(false);
  const frame = useSpriteFrame(scrubPercent, sprite);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    const bar = timelineRef.current;
    if (!bar) return;
    const rect = bar.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const percent = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setScrubPercent(percent);
  }, []);

  const handlePointerLeave = useCallback((e: React.PointerEvent) => {
    const area = scrubAreaRef.current;
    if (!area) return;
    const related = e.relatedTarget as Node | null;
    if (related && area.contains(related)) return;
    setIsHovering(false);
    setScrubPercent(null);
  }, []);

  const handlePointerEnter = useCallback(() => {
    setIsHovering(true);
  }, []);

  return (
    <section className="relative w-full flex flex-col justify-center items-center">
      {/* Full sprite sheet - decorative, shows the grid */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-80px" }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="relative overflow-hidden w-full flex justify-center items-center"
      >
        <div
          className="relative flex justify-center w-full px-5 items-center max-h-[320px] md:max-h-[400px]"
          style={{
            aspectRatio: `${sprite.columns * sprite.thumb_width} / ${sprite.rows * sprite.thumb_height}`,
          }}
        >
          <Image
            src={SPRITE_PATH}
            alt="Sprite sheet example: video frames in a grid for timeline previews"
            fill
            className="object-center select-none pointer-events-none "
            priority
          />
        </div>
      </motion.div>

      {/* Interactive timeline scrubber + preview in one hover zone */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-40px" }}
        transition={{ duration: 0.5, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
        className="mt-6 w-full"
      >
        <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider text-center mb-3">
          Hover over the timeline to see a &quot;frame&quot; preview
        </p>
        <div
          ref={scrubAreaRef}
          onPointerMove={handlePointerMove}
          onPointerLeave={handlePointerLeave}
          onPointerEnter={handlePointerEnter}
          className="relative rounded-xl border border-border/40 bg-slate-100 p-4 pt-6"
        >
          {/* Preview above the bar so cursor can stay on bar without leaving */}
          <AnimatePresence>
            {
              <motion.div
                initial={{ opacity: 0, y: 4, scale: 0.96 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 4, scale: 0.96 }}
                transition={{ type: "spring", stiffness: 400, damping: 30 }}
                className="flex flex-col items-center mb-4"
              >
                <div
                  className="relative rounded-lg border-primary/60 bg-muted shadow-xl overflow-hidden shrink-0"
                  style={{
                    width: sprite.thumb_width,
                    height: sprite.thumb_height,
                    backgroundImage: `url(${SPRITE_PATH})`,
                    backgroundSize: `1600px 900px`,
                    backgroundPosition: frame
                      ? `-${160 * frame.col}px -${90 * frame.row}px`
                      : "0px 0px",
                    backgroundRepeat: "no-repeat",
                  }}
                />
                <p className="text-center text-xs text-muted-foreground mt-2">
                  {frame ? (
                    <>
                      Frame {frame.frameIndex + 1} of {sprite.frames_count} (row{" "}
                      {frame.row + 1}, col {frame.col + 1})
                    </>
                  ) : (
                    "No frame selected"
                  )}
                </p>
              </motion.div>
            }
          </AnimatePresence>

          <div
            ref={timelineRef}
            className="relative h-4 w-full rounded-lg bg-white border border-border/60 cursor-pointer touch-none overflow-visible"
            role="slider"
            aria-label="Timeline scrubber to preview sprite sheet frames"
            aria-valuenow={scrubPercent ?? 0}
            aria-valuemin={0}
            aria-valuemax={100}
            tabIndex={0}
            onKeyDown={(e) => {
              if (!timelineRef.current) return;
              const step = 5;
              const current = scrubPercent ?? 50;
              if (e.key === "ArrowLeft")
                setScrubPercent(Math.max(0, current - step));
              if (e.key === "ArrowRight")
                setScrubPercent(Math.min(100, current + step));
            }}
          >
            <motion.div
              className="absolute inset-y-0 left-0 rounded-l-lg bg-primary/20 pointer-events-none"
              style={{ width: `${scrubPercent ?? 0}%` }}
              transition={{ type: "spring", stiffness: 400, damping: 35 }}
            />
            <AnimatePresence>
              {scrubPercent !== null && (
                <motion.div
                  initial={{ scale: 0.8, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  exit={{ scale: 0.8, opacity: 0 }}
                  transition={{ type: "spring", stiffness: 400, damping: 25 }}
                  className="absolute top-1/2 -translate-y-1/2 w-3 h-3 rounded-full bg-primary border-2 border-primary-foreground shadow-md pointer-events-none z-10"
                  style={{ left: `${scrubPercent}%`, marginLeft: -6 }}
                />
              )}
            </AnimatePresence>
          </div>
        </div>
      </motion.div>
    </section>
  );
}
