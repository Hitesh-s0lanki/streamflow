"use client";

import { useParams } from "next/navigation";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Loader2 } from "lucide-react";
import { usePlaybackSession } from "@/hooks/use-playback-session";
import { getContentDetail } from "@/lib/api/content";
import { useMediaUrl } from "@/hooks/use-media-url";
import { VideoPlayer } from "./_components/video-player";
import Image from "next/image";

export default function WatchPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const contentId = params.id;

  const {
    data: session,
    isLoading: sessionLoading,
    isError,
    error,
  } = usePlaybackSession(contentId);

  // Content detail for poster/title while session loads (cache hit when coming from content detail).
  const { data: content } = useQuery({
    queryKey: ["content", "detail", contentId],
    queryFn: () => getContentDetail(contentId!),
    enabled: !!contentId,
  });
  const posterMediaKey = content?.posterUrl ?? content?.thumbnailUrl;
  const posterUrl = useMediaUrl(posterMediaKey);

  if (sessionLoading) {
    return (
      <div className="h-screen bg-black flex flex-col items-center justify-center overflow-hidden">
        {/* Poster visible immediately so the screen isn't a generic spinner */}
        {posterUrl ? (
          <div className="absolute inset-0">
            <Image
              src={posterUrl}
              alt={content?.title ?? ""}
              fill
              className="w-full h-full object-contain"
            />
            <div className="absolute inset-0 bg-black/60" />
          </div>
        ) : null}
        <div className="relative z-10 flex flex-col items-center gap-4">
          <Loader2 className="w-10 h-10 text-white animate-spin" />
          <p className="text-white/90 text-sm font-medium">
            {posterUrl ? "Starting…" : "Preparing playback…"}
          </p>
        </div>
      </div>
    );
  }

  if (isError || !session) {
    const message =
      error instanceof Error ? error.message : "Unable to start playback.";
    return (
      <div className="h-screen bg-black flex flex-col items-center justify-center p-8 text-center">
        <p className="text-red-400 font-medium mb-2 text-lg">Playback Error</p>
        <p className="text-white/50 text-sm mb-6 max-w-md">{message}</p>
        <button
          onClick={() => router.back()}
          className="inline-flex items-center gap-2 px-5 py-2 bg-white text-black text-sm font-medium rounded-md hover:bg-white/90 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Go Back
        </button>
      </div>
    );
  }

  return <VideoPlayer session={session} />;
}
