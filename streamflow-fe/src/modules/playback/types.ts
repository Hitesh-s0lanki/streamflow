/**
 * Playback session types for the video player.
 * Maps to POST /api/media/playback/sessions response.
 */

export interface PlaybackContentInfo {
  id: string;
  title: string;
  durationSeconds: number;
  posterUrl: string | null;
  thumbnailUrl: string | null;
}

export interface PlaybackStreamInfo {
  type: "HLS";
  manifestUrl: string;
  drmEnabled: boolean;
}

export interface SpriteSheetInfo {
  sheetIndex: number;
  startTimeSeconds: number;
  endTimeSeconds: number;
  rowsCount: number;
  columnsCount: number;
  framesCount: number;
  spriteUrl: string;
}

export interface PlaybackSpritesInfo {
  intervalSeconds: number;
  thumbWidth: number;
  thumbHeight: number;
  sheets: SpriteSheetInfo[];
}

export interface PlaybackSession {
  sessionId: string;
  content: PlaybackContentInfo;
  stream: PlaybackStreamInfo;
  sprites: PlaybackSpritesInfo;
  expiresAt: string;
}

export interface CreatePlaybackSessionRequest {
  contentId: string;
}
