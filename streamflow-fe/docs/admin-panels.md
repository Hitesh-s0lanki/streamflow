# Admin Panels

## Admin-Only Screens

All admin screens live under `/admin/*`. Access is restricted to users with an admin role or flag (e.g. derived from Clerk metadata or a server-side check). Non-admin users are redirected (e.g. to home) or shown a 403-style message. Middleware or layout enforces this before rendering any admin route.

---

## Admin Content

- **List:** `/admin/content`. Calls `GET /api/admin/content` with optional query params: `publishStatus` (DRAFT | PUBLISHED), `contentType` (MOVIE | SERIES), `title` (search), `page`, `size`. Renders a paginated table or grid of content with title, type, publish status, optional thumbnail. Actions: view/edit, unpublish (for published items).
- **Edit (optional):** `/admin/content/[contentId]`. Load content with `GET /api/content/{contentId}`; update metadata with `PATCH /api/admin/content/{contentId}`. Do not allow changing contentType. Show unpublish button that calls `POST /api/admin/content/{contentId}/unpublish`.

---

## Ingestion Status Viewer

- **List:** `/admin/ingestion`. Calls `GET /api/admin/ingestion-jobs` with optional filters: `jobStatus`, `videoAssetId`, `contentId`, `from`, `to`, `page`, `size`. Display job id, videoAssetId, contentId, jobStatus, optional errorMessage, processedAt. Link to job detail.
- **Detail:** `/admin/ingestion/[jobId]`. Calls `GET /api/admin/ingestion-jobs/{jobId}` for full detail (rawS3Key, errorMessage, processedAt, jobStatus). Show current status clearly. Optionally show a “Refresh” button that re-fetches. For per-asset status during upload flow, use `GET /api/ingestion/{videoAssetId}` (see upload-flow.md).

---

## Manual Status Override

- **Purpose:** For demos or manual correction when the pipeline is stuck or skipped.
- **Endpoint:** `POST /api/admin/ingestion/{jobId}/status` with body containing the new `jobStatus` (e.g. READY). Backend enforces allowed transitions.
- **UI:** On the ingestion job detail page, show a control (e.g. “Set status”) with a dropdown of allowed statuses and a confirmation step. Use only in non-production or with clear labeling (“Demo only” / “Manual override”). After success, refresh the job detail or list.

---

## Analytics Preview

- **Overview:** `/admin/analytics` or a tab within admin. Call `GET /api/admin/analytics/overview` with optional `from` and `to` (ISO date-time). Display platform metrics (e.g. totalVideos, totalPlays, totalUniqueViewers, avgCompletionRate, avgBufferingRate, topVideos) and any ingestion readiness count returned by the admin overview.
- **Rebuild:** Provide a “Rebuild analytics” (or similar) action that calls `POST /api/admin/analytics/rebuild` with body `from` and `to`. Show a confirmation dialog (e.g. “Rebuild analytics for the selected period?”). After success, show a short success message; optionally refetch overview. Idempotent; document that in the UI if helpful.

---

## Playback Event Logs

- **Screen:** `/admin/playback-events`. Call `GET /api/admin/playback-events` with optional filters: `userId`, `videoAssetId`, `eventType`, `from`, `to`, `page`, `size`. Display a paginated table: userId, videoAssetId, eventType, currentTimeSeconds, timestamp, optional payload. Use for debugging and verifying that events are received; no edit or delete.

---

## Signed URLs Audit

- **Screen:** `/admin/signed-urls`. Call `GET /api/admin/signed-urls` with optional filters: `videoAssetId`, `urlType`, `from`, `to`, `page`, `size`. Display metadata only (no active signed URLs in response): e.g. videoAssetId, urlType, createdAt, expiresAt. For audit/debug only; no actions.

---

## Licenses (Admin)

- **List:** `/admin/licenses`. Call `GET /api/admin/licenses` with optional filters: `userId`, `videoAssetId`, `status`, `expiresFrom`, `expiresTo`, `page`, `size`. Display license id, userId, videoAssetId, status, expiresAt, createdAt.
- **Revoke:** For each row (or detail), offer “Revoke” that calls `POST /api/admin/licenses/{licenseId}/revoke`. Show confirmation (“Revoke this license? Playback will fail for this user/device.”). After success, refresh the list or update the row.

---

## Safe Demo Controls and Guardrails

- **Override ingestion status:** Label clearly as “Demo / manual override” and, if possible, restrict to non-production or require an extra confirmation. Do not allow arbitrary status jumps if the backend restricts them; only show allowed options in the UI.
- **Rebuild analytics:** Confirm before submit; show “This may take a while” or similar. Do not allow accidental repeated clicks (disable button until response).
- **Revoke license:** Always require confirmation; explain that the user will lose playback until they request a new license.
- **Unpublish content:** Confirm (“Unpublish? This will remove the title from the public catalog.”). Make it clear that the action is immediate.
- **No destructive deletes in this spec:** If the backend does not expose delete content/asset in the API, do not add delete buttons. If it does, require confirmation and optional soft-delete semantics per backend.
