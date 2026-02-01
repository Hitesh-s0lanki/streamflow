# Continue Watching

## Continue Watching Row Logic

- **Purpose:** Show a horizontal row of titles the user has started but not finished, so they can resume quickly from the home page (and optionally from a global nav).
- **Data source:** `GET /api/watch-progress/continue` with header `X-User-Id` (Clerk userId). Backend returns in-progress items only (completed=false), within a time window (e.g. last 30 days), ordered by lastWatchedAt DESC.
- **Placement:** Typically the first row on the home page below the hero (if any), or a dedicated “Continue Watching” section. Only render when the user is signed in and the API returns at least one item.

---

## Data Source

- **Endpoint:** `GET /api/watch-progress/continue`.
- **Headers:** `X-User-Id` (required; from Clerk). Without a signed-in user, do not call this endpoint; do not show the row.
- **Response:** List of continue-watching items. Each item typically includes: videoAssetId, content reference (e.g. contentId, title), optional episode/season info for series, lastWatchedSecond, durationSeconds, lastWatchedAt, and optionally thumbnail or poster URL. Exact shape follows the backend DTO (e.g. ContinueWatchingItemResponse).

---

## Sorting Rules

- **Backend order:** The API returns items ordered by lastWatchedAt DESC (most recently watched first). The frontend should display in this order without re-sorting.
- **Limit:** Backend may cap the list (e.g. last 30 days, top N). If the API supports a limit parameter, use it; otherwise display all returned items. For very long lists, consider showing a fixed number (e.g. 10) and “See all” if the product requires it.

---

## Resume Behavior

- **Click / tap:** Navigate to `/play/[videoAssetId]` with the videoAssetId from the continue-watching item. The playback page will request a license and manifest; the player will then seek to lastWatchedSecond (from watch progress) after load. The frontend does not pass the resume position in the URL; the playback page or player fetches watch progress for the asset (or receives it from the backend as part of license/playback context) and seeks to that position.
- **Display:** Show a progress bar or “X% watched” on the card using lastWatchedSecond and durationSeconds. Optional: show “Resume” label on the card.

---

## Removal When Completed

- **Backend behavior:** When watch progress is marked completed (e.g. user finishes the video, or `POST /api/watch-progress/{videoAssetId}/complete` is called), the item no longer appears in the continue-watching list because the API returns only in-progress items.
- **Frontend behavior:** After the user completes playback, the next time the home page (or the component that shows continue watching) fetches data, the completed item will be gone. No explicit “remove from list” call is needed; refetch or revalidate the continue-watching list when returning to the home page or when the user completes playback (e.g. invalidate cache or refetch).

---

## Edge Cases

### Expired License

- **Scenario:** User has an item in continue watching; when they tap “Resume,” the playback page requests a new license. If the previous license was revoked or expired, the new license request still succeeds (new license is issued). No special handling in the continue-watching row; playback-prep handles license errors. If license request fails (e.g. 403), show the error on the playback page, not in the row.

### Content Unpublished

- **Scenario:** Content was PUBLISHED when the user watched it; later an admin unpublishes it. The continue-watching API may still return the item (it is keyed by videoAssetId/userId). When the user taps “Resume,” they go to `/play/[videoAssetId]`. If the backend denies playback for unpublished content, the playback page will get 403 or 404 and show “This title is unavailable.” Optionally, the frontend can hide continue-watching items whose content is unpublished: e.g. after fetching continue watching, filter by a content visibility check (if the API returns publishStatus or a “available” flag). If the API does not return that, rely on playback denial and show the error on the playback page.

### Deleted Asset

- **Scenario:** The video asset or content was deleted. Continue-watching might still return the item; when the user taps “Resume,” playback will fail (404 or 403). Show “This title is unavailable” on the playback page. Optionally, if the API returns a 404 when fetching content detail for the item, the frontend can remove or gray out that card and show “No longer available” when the list is built; otherwise, handle it only on playback attempt.

### Empty List

- **Scenario:** User has no in-progress items. Do not show the “Continue Watching” section at all; no empty-state message required. When the user finishes all items, the row disappears on next load.
