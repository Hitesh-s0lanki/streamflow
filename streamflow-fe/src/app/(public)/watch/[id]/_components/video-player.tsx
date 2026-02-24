"use client";

import { useRef, useState, useEffect, useCallback } from "react";
import Hls from "hls.js";
import type { PlaybackSession } from "@/modules/playback/types";
import { PlayerControls } from "./player-controls";

interface VideoPlayerProps {
  session: PlaybackSession;
}

export function VideoPlayer({ session }: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const hideTimerRef = useRef<ReturnType<typeof setTimeout> | undefined>(
    undefined,
  );

  const [playing, setPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(
    session.content.durationSeconds || 0,
  );
  const [buffered, setBuffered] = useState(0);
  const [volume, setVolume] = useState(1);
  const [muted, setMuted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [qualityLevels, setQualityLevels] = useState<
    { id: number; height: number; label: string }[]
  >([]);
  const [currentQualityLevel, setCurrentQualityLevel] = useState<number>(-1);

  // --- HLS initialization ---
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const manifestUrl = session.stream?.manifestUrl;
    if (!manifestUrl) {
      queueMicrotask(() => {
        setError("No stream URL available.");
        setLoading(false);
      });
      return;
    }

    if (Hls.isSupported()) {
      const hls = new Hls({
        startLevel: -1,
        capLevelToPlayerSize: true,
      });
      hlsRef.current = hls;

      hls.loadSource(manifestUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, (_, data) => {
        setLoading(false);
        const fromManifest = (data.levels ?? []).map(
          (level: { height?: number }, index: number) => ({
            id: index,
            height: level.height ?? 0,
            label: level.height ? `${level.height}p` : "Auto",
          }),
        );
        const levels = [
          { id: -1, height: 0, label: "Auto" },
          ...fromManifest,
        ];
        setQualityLevels(levels);
        setCurrentQualityLevel(hls.currentLevel);
      });

      hls.on(Hls.Events.LEVEL_SWITCHED, (_, data) => {
        setCurrentQualityLevel(data.level);
      });

      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError();
              break;
            default:
              setError("An error occurred during playback.");
              setLoading(false);
              hls.destroy();
              break;
          }
        }
      });

      return () => {
        hls.destroy();
        hlsRef.current = null;
      };
    } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
      video.src = manifestUrl;
      video.addEventListener("loadedmetadata", () => setLoading(false));
    } else {
      queueMicrotask(() => {
        setError("HLS playback is not supported in this browser.");
        setLoading(false);
      });
    }
  }, [session.stream?.manifestUrl]);

  // --- Video event listeners ---
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const handlers = {
      timeupdate: () => setCurrentTime(video.currentTime),
      durationchange: () => {
        if (video.duration && isFinite(video.duration)) {
          setDuration(video.duration);
        }
      },
      progress: () => {
        if (video.buffered.length > 0) {
          setBuffered(video.buffered.end(video.buffered.length - 1));
        }
      },
      play: () => setPlaying(true),
      pause: () => setPlaying(false),
      waiting: () => setLoading(true),
      canplay: () => setLoading(false),
      volumechange: () => {
        setVolume(video.volume);
        setMuted(video.muted);
      },
    };

    for (const [event, handler] of Object.entries(handlers)) {
      video.addEventListener(event, handler);
    }
    return () => {
      for (const [event, handler] of Object.entries(handlers)) {
        video.removeEventListener(event, handler);
      }
    };
  }, []);

  // --- Control handlers ---
  const togglePlay = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) {
      const p = video.play();
      if (p !== undefined && typeof p.catch === "function") {
        p.catch((err: unknown) => {
          console.warn("[playback] video.play() failed", err);
          setError(
            "Playback was blocked. Try clicking play again or unmute the video.",
          );
        });
      }
    } else {
      video.pause();
    }
  }, []);

  const seek = useCallback((time: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = time;
  }, []);

  const changeVolume = useCallback((vol: number) => {
    const video = videoRef.current;
    if (!video) return;
    video.volume = vol;
    if (vol > 0) video.muted = false;
  }, []);

  const toggleMute = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = !video.muted;
  }, []);

  const toggleFullscreen = useCallback(async () => {
    const container = containerRef.current;
    if (!container) return;
    if (document.fullscreenElement) {
      await document.exitFullscreen();
    } else {
      await container.requestFullscreen();
    }
  }, []);

  const changeQuality = useCallback((levelId: number) => {
    const hls = hlsRef.current;
    if (!hls) return;
    hls.currentLevel = levelId;
    setCurrentQualityLevel(levelId);
  }, []);

  // --- Fullscreen change listener ---
  useEffect(() => {
    const onChange = () => setIsFullscreen(!!document.fullscreenElement);
    document.addEventListener("fullscreenchange", onChange);
    return () => document.removeEventListener("fullscreenchange", onChange);
  }, []);

  // --- Auto-hide controls ---
  const showControlsTemporarily = useCallback(() => {
    setShowControls(true);
    if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    hideTimerRef.current = setTimeout(() => {
      if (videoRef.current && !videoRef.current.paused) {
        setShowControls(false);
      }
    }, 3000);
  }, []);

  useEffect(() => {
    return () => {
      if (hideTimerRef.current) clearTimeout(hideTimerRef.current);
    };
  }, []);

  // --- Keyboard shortcuts ---
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement
      )
        return;

      const video = videoRef.current;
      if (!video) return;

      switch (e.key) {
        case " ":
        case "k":
          e.preventDefault();
          togglePlay();
          showControlsTemporarily();
          break;
        case "ArrowLeft":
          e.preventDefault();
          seek(Math.max(0, video.currentTime - 10));
          showControlsTemporarily();
          break;
        case "ArrowRight":
          e.preventDefault();
          seek(Math.min(video.duration || Infinity, video.currentTime + 10));
          showControlsTemporarily();
          break;
        case "ArrowUp":
          e.preventDefault();
          changeVolume(Math.min(1, video.volume + 0.1));
          showControlsTemporarily();
          break;
        case "ArrowDown":
          e.preventDefault();
          changeVolume(Math.max(0, video.volume - 0.1));
          showControlsTemporarily();
          break;
        case "m":
          toggleMute();
          showControlsTemporarily();
          break;
        case "f":
          toggleFullscreen();
          break;
        case "Escape":
          if (document.fullscreenElement) {
            document.exitFullscreen();
          }
          break;
      }
    };

    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [
    togglePlay,
    seek,
    changeVolume,
    toggleMute,
    toggleFullscreen,
    showControlsTemporarily,
  ]);

  return (
    <div
      ref={containerRef}
      className="relative w-full h-screen bg-black select-none transition-[cursor] duration-200"
      onMouseMove={showControlsTemporarily}
      style={{ cursor: showControls ? "default" : "none" }}
    >
      {/* Video element — click to toggle play */}
      <video
        ref={videoRef}
        className="w-full h-full object-contain"
        poster={session.content.posterUrl ?? undefined}
        playsInline
        onClick={togglePlay}
      />

      {/* Loading overlay — pointer-events-none so clicks reach video/controls */}
      {loading && !error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 bg-black/40 pointer-events-none transition-opacity duration-200">
          <div className="w-12 h-12 border-[3px] border-white/20 border-t-white rounded-full animate-spin" />
          <p className="text-white/90 text-sm font-medium">Loading video…</p>
        </div>
      )}

      {/* Fatal error overlay */}
      {error && (
        <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/90 z-50">
          <p className="text-white text-lg font-medium mb-2">Playback Error</p>
          <p className="text-white/60 text-sm mb-6 max-w-md text-center">
            {error}
          </p>
          <button
            onClick={() => window.history.back()}
            className="px-5 py-2 bg-white text-black text-sm font-medium rounded-md hover:bg-white/90 transition-colors"
          >
            Go Back
          </button>
        </div>
      )}

      {/* Player controls overlay */}
      <PlayerControls
        visible={showControls || !playing}
        playing={playing}
        currentTime={currentTime}
        duration={duration}
        buffered={buffered}
        volume={volume}
        muted={muted}
        isFullscreen={isFullscreen}
        title={session.content.title}
        sprites={session.sprites}
        qualityLevels={qualityLevels}
        currentQualityLevel={currentQualityLevel}
        onQualityChange={changeQuality}
        onTogglePlay={togglePlay}
        onSeek={seek}
        onVolumeChange={changeVolume}
        onToggleMute={toggleMute}
        onToggleFullscreen={toggleFullscreen}
      />
    </div>
  );
}
