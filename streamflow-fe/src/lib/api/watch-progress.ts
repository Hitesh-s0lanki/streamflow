import { getApiBaseUrl } from "@/lib/api/client";
import type { ContinueWatchingItem } from "@/types/content";

const HEADER_USER_ID = "X-User-Id";

/**
 * GET /api/watch-progress/continue — in-progress items for Continue Watching row.
 * Only call when user is signed in; pass Clerk userId for X-User-Id.
 */
export async function getContinueWatching(userId: string): Promise<ContinueWatchingItem[]> {
  const base = getApiBaseUrl();
  const url = `${base}/api/watch-progress/continue`;
  const res = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
      [HEADER_USER_ID]: userId,
    },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const message = (body as { message?: string })?.message ?? `Request failed: ${res.status}`;
    throw new Error(message);
  }
  return res.json() as Promise<ContinueWatchingItem[]>;
}
