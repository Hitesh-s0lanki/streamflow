"use client";

import type { PlaybackSpritesInfo } from "@/modules/playback/types";

interface SpritePreviewProps {
  hoverTime: number;
  hoverX: number;
  sprites: PlaybackSpritesInfo;
  containerWidth: number;
}

function formatPreviewTime(seconds: number): string {
  if (!isFinite(seconds) || seconds < 0) return "0:00";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0)
    return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  return `${m}:${String(s).padStart(2, "0")}`;
}

export function SpritePreview({
  hoverTime,
  hoverX,
  sprites,
  containerWidth,
}: SpritePreviewProps) {
  const { intervalSeconds, thumbWidth, thumbHeight, sheets } = sprites;

  if (!sheets.length || intervalSeconds <= 0) return null;

  const sheet = sheets.find(
    (s) =>
      hoverTime >= s.startTimeSeconds && hoverTime <= s.endTimeSeconds,
  );
  if (!sheet) return null;

  const frameIndex = Math.floor(hoverTime / intervalSeconds);
  const sheetStartFrame = Math.floor(sheet.startTimeSeconds / intervalSeconds);
  const localFrame = frameIndex - sheetStartFrame;
  const col = localFrame % sheet.columnsCount;
  const row = Math.floor(localFrame / sheet.columnsCount);

  const halfWidth = thumbWidth / 2;
  const clampedX = Math.max(
    halfWidth,
    Math.min(containerWidth - halfWidth, hoverX),
  );

  return (
    <div
      className="absolute bottom-full mb-4 pointer-events-none z-50"
      style={{ left: clampedX, transform: "translateX(-50%)" }}
    >
      <div className="rounded-md overflow-hidden shadow-2xl ring-1 ring-white/20">
        <div
          style={{
            width: thumbWidth,
            height: thumbHeight,
            backgroundImage: `url(${sheet.spriteUrl})`,
            backgroundPosition: `${-col * thumbWidth}px ${-row * thumbHeight}px`,
            backgroundSize: `${sheet.columnsCount * thumbWidth}px ${sheet.rowsCount * thumbHeight}px`,
            backgroundRepeat: "no-repeat",
          }}
        />
      </div>
      <div className="text-center mt-1.5">
        <span className="bg-black/80 text-white text-xs font-medium px-2 py-0.5 rounded tabular-nums">
          {formatPreviewTime(hoverTime)}
        </span>
      </div>
    </div>
  );
}
