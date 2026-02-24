import { apiFetch } from "./client";
import type {
  PlaybackSession,
  CreatePlaybackSessionRequest,
} from "@/modules/playback/types";

/** Raw playback session response (backend sends snake_case). */
interface RawPlaybackSession {
  session_id?: string;
  expires_at?: string;
  content?: {
    id?: string;
    title?: string;
    duration_seconds?: number;
    poster_url?: string | null;
    thumbnail_url?: string | null;
  };
  stream?: {
    type?: string;
    manifest_url?: string | null;
    drm_enabled?: boolean;
  };
  sprites?: {
    interval_seconds?: number;
    thumb_width?: number;
    thumb_height?: number;
    sheets?: Array<{
      sheet_index?: number;
      start_time_seconds?: number;
      end_time_seconds?: number;
      rows_count?: number;
      columns_count?: number;
      frames_count?: number;
      sprite_url?: string;
    }>;
  };
}

function mapPlaybackSession(raw: RawPlaybackSession): PlaybackSession {
  return {
    sessionId: raw.session_id ?? "",
    expiresAt: raw.expires_at ?? "",
    content: raw.content
      ? {
          id: raw.content.id ?? "",
          title: raw.content.title ?? "",
          durationSeconds: raw.content.duration_seconds ?? 0,
          posterUrl: raw.content.poster_url ?? null,
          thumbnailUrl: raw.content.thumbnail_url ?? null,
        }
      : { id: "", title: "", durationSeconds: 0, posterUrl: null, thumbnailUrl: null },
    stream: raw.stream
      ? {
          type: (raw.stream.type as "HLS") ?? "HLS",
          manifestUrl: raw.stream.manifest_url ?? "",
          drmEnabled: raw.stream.drm_enabled ?? false,
        }
      : { type: "HLS", manifestUrl: "", drmEnabled: false },
    sprites: raw.sprites
      ? {
          intervalSeconds: raw.sprites.interval_seconds ?? 0,
          thumbWidth: raw.sprites.thumb_width ?? 0,
          thumbHeight: raw.sprites.thumb_height ?? 0,
          sheets:
            raw.sprites.sheets?.map((s) => ({
              sheetIndex: s.sheet_index ?? 0,
              startTimeSeconds: s.start_time_seconds ?? 0,
              endTimeSeconds: s.end_time_seconds ?? 0,
              rowsCount: s.rows_count ?? 0,
              columnsCount: s.columns_count ?? 0,
              framesCount: s.frames_count ?? 0,
              spriteUrl: s.sprite_url ?? "",
            })) ?? [],
        }
      : { intervalSeconds: 0, thumbWidth: 0, thumbHeight: 0, sheets: [] },
  };
}

/**
 * POST /api/media/playback/sessions — create a playback session with
 * pre-signed HLS manifest and sprite URLs.
 * Backend returns snake_case; we map to camelCase for the app.
 */
export async function createPlaybackSession(
  request: CreatePlaybackSessionRequest,
): Promise<PlaybackSession> {
  const raw = await apiFetch<RawPlaybackSession>("/api/media/playback/sessions", {
    method: "POST",
    body: JSON.stringify(request),
  });

  const session = mapPlaybackSession(raw);

  if (!session.stream.manifestUrl) {
    console.warn(
      "[playback] Backend returned no manifest_url — player will stay in loading. contentId=",
      request.contentId,
    );
  }

  return session;
}
