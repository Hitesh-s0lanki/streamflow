import { useQuery } from "@tanstack/react-query";
import { getMediaUrl } from "@/lib/api/media";

/**
 * Resolve an S3 object key to a presigned URL via GET /api/media/url.
 * Returns `undefined` while loading or if key is null/empty.
 * Results are cached for 50 minutes (presigned URLs default to 60 min expiry).
 */
export function useMediaUrl(key: string | null | undefined) {
  const { data } = useQuery({
    queryKey: ["media-url", key],
    queryFn: () => getMediaUrl(key!),
    enabled: !!key,
    staleTime: 50 * 60 * 1000,
    gcTime: 55 * 60 * 1000,
  });

  return data?.url ?? undefined;
}
