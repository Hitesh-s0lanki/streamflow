import { useQuery } from "@tanstack/react-query";
import { createPlaybackSession } from "@/lib/api/playback";

/**
 * Fetch a playback session for the given content.
 * Cached for 25 min (manifest URL expires in 30 min).
 */
export function usePlaybackSession(contentId: string | undefined) {
  return useQuery({
    queryKey: ["playback-session", contentId],
    queryFn: () => createPlaybackSession({ contentId: contentId! }),
    enabled: !!contentId,
    staleTime: 25 * 60 * 1000,
    gcTime: 28 * 60 * 1000,
    retry: 1,
  });
}
