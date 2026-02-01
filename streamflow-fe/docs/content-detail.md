# Content Detail Page

## Purpose

The content detail page shows metadata and play/resume actions for a single piece of content (movie or series). It is the bridge between catalog and playback: user chooses what to play and from where (e.g. which episode, resume or start over).

---

## Movie Detail Page Behavior

- **Data:** `GET /api/content/{contentId}` returns the content with contentType MOVIE and its single video asset (or reference to it). Optionally fetch watch progress: `GET /api/watch-progress/{videoAssetId}` when the user is signed in.
- **Layout:** Hero with poster/backdrop, title, description, release year, rating, duration. Single primary CTA: Play or Resume (see CTA logic below).
- **No season/episode UI:** Do not show season selector or episode list for movies.

---

## Series Detail Page Behavior

- **Data:** `GET /api/content/{contentId}`; then `GET /api/content/{contentId}/seasons` for the list of seasons. For the selected season, `GET /api/seasons/{seasonId}/episodes` for the episode list. For each episode (or for the “next” episode), optionally fetch watch progress per videoAssetId when the user is signed in.
- **Layout:** Same hero area as movie (title, description, release year, rating). Below: season selector (tabs or dropdown); then episode list (cards or list rows) with episode number, title, duration, thumbnail. Each episode has a Play/Resume CTA using that episode’s videoAssetId.
- **Default season:** On load, select the first season (e.g. seasonNumber 1) and fetch its episodes. User can switch season; refetch episodes for the new seasonId.

---

## Differences Between MOVIE and SERIES UI

| Aspect | MOVIE | SERIES |
|--------|-------|--------|
| Hero CTA | One “Play” or “Resume” for the single video asset | Optional: “Play” for first episode or “Continue” for next in progress; or no hero play and rely on episode list |
| Season selector | Hidden | Shown; switching changes the episode list |
| Episode list | Hidden | Shown for selected season |
| Play target | Single videoAssetId | One videoAssetId per episode; user chooses episode |

---

## Season Selector Behavior

- **Source:** List from `GET /api/content/{contentId}/seasons` (ordered by seasonNumber).
- **Interaction:** User selects a season (e.g. “Season 1”, “Season 2”). On change, request `GET /api/seasons/{seasonId}/episodes` and replace the episode list. Selected season is clearly highlighted.
- **State:** Store selected seasonId in component state (or URL if product wants shareable links per season). Default to first season on initial load.

---

## Episode List Behavior

- **Source:** `GET /api/seasons/{seasonId}/episodes` (ordered by episodeNumber).
- **Display:** For each episode: number, title, duration, thumbnail (thumbnailUrl). Show a Play or Resume button that navigates to `/play/[videoAssetId]` with that episode’s videoAssetId.
- **Progress:** If signed in, use watch progress per videoAssetId to show “Resume” and optionally progress bar or “X% watched”; otherwise show “Play.”

---

## Metadata Displayed

- **From content:** Title, description, releaseYear, rating, posterUrl, thumbnailUrl, durationSeconds (for movie or aggregated if API provides).
- **From season (series):** Season number, optional season title, posterUrl if needed.
- **From episode:** Episode number, title, description, durationSeconds, thumbnailUrl.
- Do not show contentId, videoAssetId, or internal status in the main UI; use them only for navigation and API calls.

---

## CTA Logic: Play, Resume, Continue Watching

- **Play:** Shown when there is no watch progress or progress is 0 or “start over” is chosen. Navigate to `/play/[videoAssetId]` and let the playback page handle license and start from 0.
- **Resume:** Shown when watch progress exists for that videoAssetId and completed is false. Navigate to `/play/[videoAssetId]`; the playback page will request the license and manifest, then the player will seek to lastWatchedSecond (from watch progress) after load.
- **Continue Watching (series):** If the product uses a “Continue Watching” concept on the detail page (e.g. “Continue S1 E3”), determine the next episode from watch progress (e.g. last in-progress episode’s videoAssetId) and show one prominent “Continue” CTA that goes to `/play/[videoAssetId]` for that asset. Alternatively, the hero can show “Resume” for the next episode’s asset if that is the only in-progress item.
- **Completed:** When watch progress has completed=true for that asset, show “Play” again (or “Replay”) so the user can start over; do not show “Resume” for completed items.

---

## API Calls Summary

- **All:** `GET /api/content/{contentId}`.
- **Movie:** Optional `GET /api/watch-progress/{videoAssetId}` for the movie’s video asset (when signed in).
- **Series:** `GET /api/content/{contentId}/seasons`; then for selected season `GET /api/seasons/{seasonId}/episodes`. For each episode that needs a Play/Resume label, optional `GET /api/watch-progress/{videoAssetId}` when signed in (can be batched or requested on demand to avoid N+1).
