# Player Behavior

## Player Lifecycle

1. **Mount:** Player mounts only after the playback-prep flow has provided a valid signed manifest URL. It receives the URL as the source and, if applicable, the initial seek position (from watch progress).
2. **Load:** Player loads the manifest (HLS/DASH) and begins buffering. Show a loading indicator until the first frame is playable or until a timeout.
3. **Playback:** User can play, pause, seek, change volume. The frontend emits playback events and periodically upserts watch progress.
4. **Teardown:** On route leave or unmount, emit final progress and, if the user left before completion, an “abandoned” event (if the product defines it). Do not leave the player running in the background when the user has navigated away.

---

## Events Emitted

The frontend must send playback events to the backend for analytics. Backend endpoint: `POST /api/playback/events` with body containing at least: event type, videoAssetId, currentTimeSeconds, and optionally userId (or rely on backend to infer from header).

- **play:** When playback starts or resumes. Include currentTimeSeconds at the moment of play.
- **pause:** When playback is paused. Include currentTimeSeconds.
- **seek:** When the user seeks. Include the new currentTimeSeconds after the seek. Optionally throttle or debounce rapid seeks to avoid flooding the API.
- **buffering:** When the player enters a buffering state. Include currentTimeSeconds. Optionally send once per buffering spell or at interval while buffering.
- **completed:** When playback reaches the end (e.g. currentTime >= duration). Send once; include currentTimeSeconds (or duration).
- **abandoned:** When the user leaves the page or closes the tab without completing. Send when unmounting or on beforeunload if the video was playing and not completed. Include last known currentTimeSeconds.

Emit these from the player component or a small playback hook that has access to the player’s current time and state. Use the existing event types defined by the backend (e.g. PLAY, PAUSE, SEEK, BUFFERING, COMPLETED, ABANDONED, ERROR).

---

## When Watch Progress Is Updated

- **During playback:** Periodically (e.g. every 10–30 seconds) or on pause/seek, call `POST /api/watch-progress` with videoAssetId, lastWatchedSecond (current playback position), and completed=false. Use the player’s current time. Do not send on every second; throttle to reduce load.
- **On pause / leave:** Send a final upsert with the current position so the user can resume later.
- **On completion:** When the user reaches the end, call `POST /api/watch-progress/{videoAssetId}/complete` (or equivalent) to mark the asset as completed. Also send the “completed” playback event. After that, do not send further progress updates for that session for that asset.

---

## How Completion Is Detected

- **Player event:** The HLS/DASH player typically fires an “ended” or “complete” event when currentTime reaches duration (or the last segment ends). When that fires, the frontend (1) sends the “completed” playback event, (2) marks watch progress as completed via the backend (e.g. `POST /api/watch-progress/{videoAssetId}/complete` or upsert with completed=true), and (3) can show “Replay” or redirect back to content detail after a short delay.
- **Threshold:** If the player does not fire exactly at the end, consider “completed” when currentTime is within a few seconds of duration (e.g. >= duration - 2). Use a single firing so the backend is not called multiple times for completion.

---

## How Sprite Sheets Are Used During Seek

- **Seek bar hover / scrub:** While the user hovers or drags over the seek bar, the frontend maps the seek position (time in seconds) to a sprite frame using the sprite metadata from `GET /api/video-assets/{videoAssetId}/sprites` (and optionally `GET /api/sprites/{spriteSheetId}/frames` for per-frame mapping). Display the corresponding thumbnail in a tooltip or preview above the seek bar. See sprite-preview.md for mapping and fallbacks.
- **No playback during scrub:** The actual playback position does not change until the user releases; only the preview thumbnail updates. When the user releases, the player seeks to that time.

---

## Failure Recovery Behavior

- **Manifest or segment load failure:** If the player fails to load the manifest or a segment (e.g. 403, 404, network error), treat as license/URL expiry or temporary failure. Stop the player, show “Playback error” with a “Try again” button that re-runs the playback-prep flow (new license, new manifest URL) and then remounts the player. Optionally retry once automatically before showing the error.
- **Buffering timeout:** If the player is stuck buffering for too long (e.g. 30 s), show “Buffering… taking longer than usual” and offer “Retry” (reload source) or “Back to show.”
- **Decode/playback error:** If the player fires an error event (e.g. decode error), show a generic “Playback failed” message and “Try again” or “Back to show.” Do not expose technical error details to the user unless in a debug mode.
