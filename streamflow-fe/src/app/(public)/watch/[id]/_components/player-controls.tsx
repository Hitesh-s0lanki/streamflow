"use client";

import { useRef, useState, useCallback, useEffect, useLayoutEffect } from "react";
import { useRouter } from "next/navigation";
import {
  Play,
  Pause,
  Volume2,
  VolumeX,
  Volume1,
  Maximize,
  Minimize,
  ArrowLeft,
  RotateCcw,
  RotateCw,
  Settings2,
  Check,
} from "lucide-react";
import type { PlaybackSpritesInfo } from "@/modules/playback/types";
import { SpritePreview } from "./sprite-preview";

export interface QualityLevel {
  id: number;
  height: number;
  label: string;
}

interface PlayerControlsProps {
  visible: boolean;
  playing: boolean;
  currentTime: number;
  duration: number;
  buffered: number;
  volume: number;
  muted: boolean;
  isFullscreen: boolean;
  title: string;
  sprites: PlaybackSpritesInfo;
  qualityLevels?: QualityLevel[];
  currentQualityLevel?: number;
  onQualityChange?: (levelId: number) => void;
  onTogglePlay: () => void;
  onSeek: (time: number) => void;
  onVolumeChange: (volume: number) => void;
  onToggleMute: () => void;
  onToggleFullscreen: () => void;
}

function formatTime(seconds: number): string {
  if (!isFinite(seconds) || seconds < 0) return "0:00";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  if (h > 0)
    return `${h}:${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
  return `${m}:${String(s).padStart(2, "0")}`;
}

export function PlayerControls({
  visible,
  playing,
  currentTime,
  duration,
  buffered,
  volume,
  muted,
  isFullscreen,
  title,
  sprites,
  qualityLevels = [],
  currentQualityLevel = -1,
  onQualityChange,
  onTogglePlay,
  onSeek,
  onVolumeChange,
  onToggleMute,
  onToggleFullscreen,
}: PlayerControlsProps) {
  const router = useRouter();
  const seekBarRef = useRef<HTMLDivElement>(null);
  const [hoverTime, setHoverTime] = useState<number | null>(null);
  const [hoverX, setHoverX] = useState(0);
  const [isSeeking, setIsSeeking] = useState(false);
  const [showVolumeSlider, setShowVolumeSlider] = useState(false);
  const [showQualityMenu, setShowQualityMenu] = useState(false);
  const [seekBarWidth, setSeekBarWidth] = useState(0);
  const qualityMenuRef = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    const bar = seekBarRef.current;
    if (!bar) return;
    const updateWidth = () => setSeekBarWidth(bar.clientWidth);
    updateWidth();
    const ro = new ResizeObserver(updateWidth);
    ro.observe(bar);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    if (!showQualityMenu) return;
    const handleClickOutside = (e: MouseEvent) => {
      if (
        qualityMenuRef.current &&
        !qualityMenuRef.current.contains(e.target as Node)
      ) {
        setShowQualityMenu(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [showQualityMenu]);

  const progress = duration > 0 ? (currentTime / duration) * 100 : 0;
  const bufferedProgress = duration > 0 ? (buffered / duration) * 100 : 0;

  const getTimeFromPosition = useCallback(
    (clientX: number) => {
      const bar = seekBarRef.current;
      if (!bar || duration <= 0) return 0;
      const rect = bar.getBoundingClientRect();
      const ratio = Math.max(
        0,
        Math.min(1, (clientX - rect.left) / rect.width),
      );
      return ratio * duration;
    },
    [duration],
  );

  const handleSeekBarMouseMove = useCallback(
    (e: React.MouseEvent) => {
      const time = getTimeFromPosition(e.clientX);
      setHoverTime(time);
      setHoverX(
        e.clientX - (seekBarRef.current?.getBoundingClientRect().left ?? 0),
      );
    },
    [getTimeFromPosition],
  );

  const handleSeekBarMouseDown = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      setIsSeeking(true);
      const time = getTimeFromPosition(e.clientX);
      onSeek(time);
    },
    [getTimeFromPosition, onSeek],
  );

  const handleSeekBarMouseLeave = useCallback(() => {
    if (!isSeeking) setHoverTime(null);
  }, [isSeeking]);

  useEffect(() => {
    if (!isSeeking) return;

    const handleMouseMove = (e: MouseEvent) => {
      const time = getTimeFromPosition(e.clientX);
      setHoverTime(time);
      setHoverX(
        e.clientX - (seekBarRef.current?.getBoundingClientRect().left ?? 0),
      );
      onSeek(time);
    };

    const handleMouseUp = () => {
      setIsSeeking(false);
      setHoverTime(null);
    };

    window.addEventListener("mousemove", handleMouseMove);
    window.addEventListener("mouseup", handleMouseUp);
    return () => {
      window.removeEventListener("mousemove", handleMouseMove);
      window.removeEventListener("mouseup", handleMouseUp);
    };
  }, [isSeeking, getTimeFromPosition, onSeek]);

  const VolumeIcon =
    muted || volume === 0 ? VolumeX : volume < 0.5 ? Volume1 : Volume2;

  const currentQualityLabel =
    qualityLevels.find((l) => l.id === currentQualityLevel)?.label ?? "Auto";

  return (
    <div
      className={`absolute inset-0 flex flex-col justify-between transition-[opacity,visibility] duration-300 ease-out ${
        visible ? "opacity-100 visible" : "opacity-0 invisible"
      }`}
      style={{
        pointerEvents: visible ? undefined : "none",
        willChange: visible ? "auto" : "opacity",
      }}
    >
      {/* Gradients (visual only) */}
      <div className="absolute top-0 inset-x-0 h-32 bg-linear-to-b from-black/70 to-transparent pointer-events-none" />
      <div className="absolute bottom-0 inset-x-0 h-48 bg-linear-to-t from-black/80 to-transparent pointer-events-none" />

      {/* Top bar — back + title */}
      <div className="relative z-10 flex items-center gap-3 pt-4 px-4 md:px-8 pointer-events-auto">
        <button
          onClick={() => router.back()}
          className="p-2 rounded-full hover:bg-white/10 transition-colors"
          aria-label="Go back"
        >
          <ArrowLeft className="w-5 h-5 text-white" />
        </button>
        <h2 className="text-white text-base md:text-lg font-medium truncate">
          {title}
        </h2>
      </div>

      {/* Center play controls */}
      <div className="relative z-10 flex items-center justify-center gap-10 pointer-events-none">
        <button
          onClick={() => onSeek(Math.max(0, currentTime - 10))}
          className="pointer-events-auto p-3 rounded-full hover:bg-white/10 transition-colors"
          aria-label="Rewind 10 seconds"
        >
          <RotateCcw className="w-7 h-7 text-white" />
        </button>
        <button
          onClick={onTogglePlay}
          className="pointer-events-auto p-5 rounded-full bg-white/15 hover:bg-white/25 backdrop-blur-sm transition-colors"
          aria-label={playing ? "Pause" : "Play"}
        >
          {playing ? (
            <Pause className="w-9 h-9 text-white" />
          ) : (
            <Play className="w-9 h-9 text-white ml-0.5" />
          )}
        </button>
        <button
          onClick={() => onSeek(Math.min(duration, currentTime + 10))}
          className="pointer-events-auto p-3 rounded-full hover:bg-white/10 transition-colors"
          aria-label="Forward 10 seconds"
        >
          <RotateCw className="w-7 h-7 text-white" />
        </button>
      </div>

      {/* Bottom controls */}
      <div className="relative z-10 pb-4 px-4 md:px-8 pointer-events-auto">
        {/* Seek bar */}
        <div className="relative mb-2">
          {hoverTime !== null && sprites.sheets.length > 0 && (
            <SpritePreview
              hoverTime={hoverTime}
              hoverX={hoverX}
              sprites={sprites}
              containerWidth={seekBarWidth}
            />
          )}

          <div
            ref={seekBarRef}
            className="group/seek relative h-[5px] hover:h-[7px] transition-[height] cursor-pointer select-none"
            onMouseMove={handleSeekBarMouseMove}
            onMouseDown={handleSeekBarMouseDown}
            onMouseLeave={handleSeekBarMouseLeave}
          >
            {/* Track background */}
            <div className="absolute inset-0 bg-white/25 rounded-full" />

            {/* Buffered range */}
            <div
              className="absolute inset-y-0 left-0 bg-white/40 rounded-full"
              style={{ width: `${bufferedProgress}%` }}
            />

            {/* Hover fill */}
            {hoverTime !== null && (
              <div
                className="absolute inset-y-0 left-0 bg-white/20 rounded-full"
                style={{ width: `${(hoverTime / duration) * 100}%` }}
              />
            )}

            {/* Progress */}
            <div
              className="absolute inset-y-0 left-0 bg-primary rounded-full"
              style={{ width: `${progress}%` }}
            />

            {/* Thumb */}
            <div
              className="absolute top-1/2 w-3.5 h-3.5 bg-primary rounded-full scale-0 group-hover/seek:scale-100 transition-transform shadow-lg"
              style={{
                left: `${progress}%`,
                transform: `translate(-50%, -50%) scale(var(--tw-scale-x, 0))`,
              }}
            />
          </div>
        </div>

        {/* Controls bar */}
        <div className="flex items-center justify-between text-white">
          <div className="flex items-center gap-2">
            <button
              onClick={onTogglePlay}
              className="p-1.5 hover:bg-white/10 rounded-md transition-colors"
              aria-label={playing ? "Pause" : "Play"}
            >
              {playing ? (
                <Pause className="w-5 h-5" />
              ) : (
                <Play className="w-5 h-5" />
              )}
            </button>

            <button
              onClick={() => onSeek(Math.min(duration, currentTime + 10))}
              className="p-1.5 hover:bg-white/10 rounded-md transition-colors"
              aria-label="Forward 10 seconds"
            >
              <RotateCw className="w-5 h-5" />
            </button>

            {/* Volume */}
            <div
              className="relative flex items-center"
              onMouseEnter={() => setShowVolumeSlider(true)}
              onMouseLeave={() => setShowVolumeSlider(false)}
            >
              <button
                onClick={onToggleMute}
                className="p-1.5 hover:bg-white/10 rounded-md transition-colors"
                aria-label={muted ? "Unmute" : "Mute"}
              >
                <VolumeIcon className="w-5 h-5" />
              </button>
              <div
                className={`ml-1 overflow-hidden transition-all duration-200 ${
                  showVolumeSlider ? "w-20 opacity-100" : "w-0 opacity-0"
                }`}
              >
                <input
                  type="range"
                  min={0}
                  max={1}
                  step={0.01}
                  value={muted ? 0 : volume}
                  onChange={(e) => onVolumeChange(parseFloat(e.target.value))}
                  className="w-full h-1 cursor-pointer accent-primary"
                />
              </div>
            </div>

            <span className="text-sm tabular-nums select-none ml-1 text-white/80">
              {formatTime(currentTime)}
              <span className="text-white/40"> / </span>
              {formatTime(duration)}
            </span>
          </div>

          <div className="flex items-center gap-1">
            {/* Quality selector */}
            {qualityLevels.length > 0 && onQualityChange && (
              <div className="relative" ref={qualityMenuRef}>
                <button
                  type="button"
                  onClick={() => setShowQualityMenu((v) => !v)}
                  className="flex items-center gap-1.5 px-2 py-1.5 hover:bg-white/10 rounded-md transition-colors duration-150 ease-out"
                  aria-label="Quality"
                  aria-expanded={showQualityMenu}
                >
                  <Settings2 className="w-5 h-5 text-white/90" />
                  <span className="text-sm text-white/90 tabular-nums">
                    {currentQualityLabel}
                  </span>
                </button>
                <div
                  className={`absolute bottom-full right-0 mb-1 min-w-28 py-1 rounded-lg bg-black/90 backdrop-blur-sm border border-white/10 shadow-xl overflow-hidden transition-[opacity,transform] duration-200 ease-out ${
                    showQualityMenu
                      ? "opacity-100 scale-100"
                      : "opacity-0 scale-95 pointer-events-none"
                  }`}
                >
                  {qualityLevels.map((level) => (
                    <button
                      key={level.id}
                      type="button"
                      onClick={() => {
                        onQualityChange(level.id);
                        setShowQualityMenu(false);
                      }}
                      className="flex items-center justify-between w-full px-3 py-2 text-left text-sm text-white/90 hover:bg-white/10 transition-colors duration-150"
                    >
                      <span>{level.label}</span>
                      {currentQualityLevel === level.id && (
                        <Check className="w-4 h-4 text-primary shrink-0" />
                      )}
                    </button>
                  ))}
                </div>
              </div>
            )}
            <button
              onClick={onToggleFullscreen}
              className="p-1.5 hover:bg-white/10 rounded-md transition-colors duration-150 ease-out"
              aria-label={isFullscreen ? "Exit fullscreen" : "Fullscreen"}
            >
              {isFullscreen ? (
                <Minimize className="w-5 h-5" />
              ) : (
                <Maximize className="w-5 h-5" />
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
