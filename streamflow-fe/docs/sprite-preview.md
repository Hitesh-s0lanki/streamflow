# Sprite Preview (Seek Bar)

## Sprite Sheet Usage Model

- **Purpose:** Netflix-style seek preview: when the user hovers or scrubs over the seek bar, show a thumbnail for that time position instead of a generic icon or nothing.
- **Data source:** Backend exposes sprite metadata per video asset. Frontend fetches `GET /api/video-assets/{videoAssetId}/sprites` to get a list of sprite sheets. Each sprite sheet has: spriteUrl, startTimeSeconds, endTimeSeconds, columns, rows, thumbnailWidth, thumbnailHeight, optional intervalSeconds, and an id (spriteSheetId). Optionally, per-frame metadata is available via `GET /api/sprites/{spriteSheetId}/frames` (frameIndex, timeOffsetSeconds, xPosition, yPosition, width, height) for precise mapping.
- **Usage:** The frontend uses this metadata to map a seek time (in seconds) to a specific frame in a sprite image and displays that frame in a preview tooltip or overlay near the seek bar.

---

## Mapping Seek Time to Sprite Frame

1. **Find the sprite sheet:** Given the current seek time `t`, find the sprite sheet whose range covers `t` (startTimeSeconds <= t <= endTimeSeconds). If multiple sheets exist (e.g. one per 5 minutes), pick the one that contains `t`.
2. **Frame index within the sheet:**  
   - If intervalSeconds is present: frame index = floor((t - startTimeSeconds) / intervalSeconds), clamped to the number of frames in the sheet (columns * rows).  
   - If per-frame metadata is used: fetch or use cached frames for that spriteSheetId; find the frame whose timeOffsetSeconds is <= t and closest to t (or the frame that contains t).
3. **Position in the image:**  
   - Grid layout: row = frameIndex / columns, col = frameIndex % columns; x = col * thumbnailWidth, y = row * thumbnailHeight. Crop or display the region (x, y, thumbnailWidth, thumbnailHeight) from spriteUrl.  
   - Per-frame metadata: use xPosition, yPosition, width, height from the matching frame to crop the sprite image.
4. **Display:** Show the cropped region (e.g. in a small preview div or canvas) near the seek bar (e.g. above the cursor). Update on hover move or scrub; hide when the user leaves the seek bar.

---

## Handling Multiple Sprite Sheets

- A video asset can have multiple sprite sheets (e.g. one per segment or time range). Each has startTimeSeconds and endTimeSeconds.
- For a given seek time `t`, select the sheet where startTimeSeconds <= t <= endTimeSeconds. If `t` falls between sheets (e.g. gap), use the previous sheet’s last frame or the next sheet’s first frame, or show a fallback (see below).
- Preload or lazy-load sprite images: for long videos, only load the sprite sheet that covers the current hover/scrub range to save bandwidth; when the user moves to another range, load that sheet.

---

## Fallback Behavior If Sprites Missing

- **No sprite data:** If `GET /api/video-assets/{videoAssetId}/sprites` returns an empty list or the asset has no sprites, do not show a time-based thumbnail. Show a generic placeholder (e.g. video icon, poster image, or “Preview not available”) in the seek preview area, or hide the preview.
- **Time outside all ranges:** If the seek time is before the first sheet’s start or after the last sheet’s end, show the first or last frame, or the same generic placeholder.
- **Image load failure:** If spriteUrl fails to load (404, CORS, network), show the same placeholder and do not retry indefinitely.

---

## Performance Considerations

- **Lazy load sprite images:** Load sprite images only when needed (e.g. when the user first hovers over the seek bar, or when the hover time falls in a new sheet’s range). Do not load all sprite sheets for a long video at once.
- **Cache:** Cache sprite metadata (and optionally decoded/cropped thumbnails) in memory for the current playback session so repeated hovers over the same range do not re-fetch or re-decode.
- **Debounce / throttle:** Update the preview on hover move with a small throttle (e.g. 50–100 ms) to avoid excessive layout and paint. When scrubbing, update every frame or at a fixed interval that feels responsive.
- **Canvas vs CSS:** Use a small canvas or a clipped img/crop to show the frame; avoid loading many small images per frame. Prefer one sprite image per sheet and crop in JS or CSS (object-position, clip) for efficiency.

---

## Mobile vs Desktop Behavior

- **Desktop:** Hover over the seek bar shows the preview at the cursor; scrub (click-drag) updates the preview. Standard behavior as above.
- **Mobile:** No hover; use “scrub” (touch drag) on the seek bar to show the preview. Show the preview above or near the touch point while the user drags; hide when the user releases. Use the same mapping and fallback logic. Consider slightly larger touch target for the seek bar and a larger preview for touch devices.
- **Performance on mobile:** Be conservative with sprite preloading; load only the sheet for the current scrub range. Use smaller preview size if needed to keep rendering smooth.
