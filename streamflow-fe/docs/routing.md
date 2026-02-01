# Routing

## Full Route Map

| Route | Purpose | Auth | Data / Backend |
|-------|---------|------|----------------|
| `/` | Home: catalog + continue watching | Optional (public catalog; continue watching if signed in) | `GET /api/content`, `GET /api/watch-progress/continue` |
| `/content/[contentId]` | Content detail (movie or series) | Public | `GET /api/content/{contentId}`, `GET /api/content/{contentId}/seasons`, `GET /api/seasons/{seasonId}/episodes` (per season), `GET /api/watch-progress/{videoAssetId}` per asset |
| `/play/[videoAssetId]` | Playback page | Protected | License + manifest URL (see playback-prep) |
| `/admin` | Admin dashboard / entry | Admin only | Optional: `GET /api/admin/analytics/overview` or health |
| `/admin/content` | Admin content list | Admin only | `GET /api/admin/content` (paginated, filters) |
| `/admin/content/[contentId]` | Admin content edit (optional) | Admin only | `GET /api/content/{contentId}`, `PATCH /api/admin/content/{contentId}` |
| `/admin/upload` | Admin upload flow (create content → upload → confirm) | Admin only | Content, season/episode, video-asset, upload-url, ingestion (see upload-flow) |
| `/admin/ingestion` | Ingestion jobs list + status | Admin only | `GET /api/admin/ingestion-jobs`, `GET /api/ingestion/{videoAssetId}` |
| `/admin/ingestion/[jobId]` | Ingestion job detail + override | Admin only | `GET /api/admin/ingestion-jobs/{jobId}`, `POST /api/admin/ingestion/{jobId}/status` |
| `/admin/analytics` | Analytics overview + rebuild | Admin only | `GET /api/admin/analytics/overview`, `POST /api/admin/analytics/rebuild` |
| `/admin/playback-events` | Playback event logs | Admin only | `GET /api/admin/playback-events` |
| `/admin/licenses` | License list + revoke | Admin only | `GET /api/admin/licenses`, `POST /api/admin/licenses/{licenseId}/revoke` |
| `/admin/signed-urls` | Signed URL audit | Admin only | `GET /api/admin/signed-urls` |
| `/api/health` (or external) | Health check | None | `GET /api/health`, `GET /api/ready` (optional) |

---

## Public vs Protected Routes

- **Public:** `/`, `/content/[contentId]`. Catalog and detail are visible without signing in. Continue watching and playback require sign-in; those sections either hide or show a sign-in prompt when the user is not authenticated.
- **Protected (signed-in user):** `/play/[videoAssetId]`. Middleware or layout checks Clerk auth; if not signed in, redirect to sign-in or show a clear CTA.
- **Admin-only:** All routes under `/admin/*`. Middleware or layout checks an admin role/flag (e.g. from Clerk metadata or a dedicated admin list). Non-admin users are redirected or shown 403.

---

## Dynamic Routes

### Content

- **Pattern:** `/content/[contentId]`
- **contentId:** UUID of the content (movie or series).
- **Fetches:** Content detail (`GET /api/content/{contentId}`), seasons if series (`GET /api/content/{contentId}/seasons`), and for the selected season, episodes (`GET /api/seasons/{seasonId}/episodes`). For each playable video asset, watch progress can be fetched (`GET /api/watch-progress/{videoAssetId}`) when the user is signed in.

### Seasons

- No dedicated season route; seasons are selected on the content detail page. Episode list is fetched by `seasonId`: `GET /api/seasons/{seasonId}/episodes`.

### Episodes

- Episodes are listed on the content detail page; each episode has a `videoAssetId` used for “Play” and “Resume”. No standalone episode URL is required; playback uses `videoAssetId` at `/play/[videoAssetId]`.

### Playback

- **Pattern:** `/play/[videoAssetId]`
- **videoAssetId:** UUID of the video asset (movie’s single asset or an episode’s asset).
- **Fetches:** No initial GET for the page itself; the page runs the playback-prep flow (license, manifest URL) and then mounts the player with the signed URL.

---

## URL Patterns Summary

- Home: `/`
- Content detail: `/content/[contentId]`
- Playback: `/play/[videoAssetId]`
- Admin: `/admin`, `/admin/content`, `/admin/content/[contentId]`, `/admin/upload`, `/admin/ingestion`, `/admin/ingestion/[jobId]`, `/admin/analytics`, `/admin/playback-events`, `/admin/licenses`, `/admin/signed-urls`

---

## Data and Backend Endpoints per Route

- **`/`:** `GET /api/content` (catalog, PUBLISHED only); if signed in, `GET /api/watch-progress/continue` (continue watching row).
- **`/content/[contentId]`:** `GET /api/content/{contentId}`; if series, `GET /api/content/{contentId}/seasons`; then for active season `GET /api/seasons/{seasonId}/episodes`. For each relevant video asset, `GET /api/watch-progress/{videoAssetId}` when user is signed in.
- **`/play/[videoAssetId]`:** `POST /api/playback/license` (body: `videoAssetId`, optional `deviceId`); then `POST /api/playback/{videoAssetId}/manifest-url` (body: `licenseId`). Optional: `GET /api/playback/license/{licenseId}` to validate before requesting manifest. Progress: `POST /api/watch-progress`, `POST /api/playback/events` (from player).
- **Admin content list:** `GET /api/admin/content` (query: `publishStatus`, `contentType`, `title`, `page`, `size`).
- **Admin content update:** `PATCH /api/admin/content/{contentId}`; unpublish: `POST /api/admin/content/{contentId}/unpublish`.
- **Admin upload flow:** See `upload-flow.md` (content, seasons, episodes, video asset, upload URL, confirm upload, ingestion status).
- **Admin ingestion:** `GET /api/admin/ingestion-jobs` (filters, pagination); `GET /api/admin/ingestion-jobs/{jobId}`; `POST /api/admin/ingestion/{jobId}/status` (override).
- **Admin analytics:** `GET /api/admin/analytics/overview`; `POST /api/admin/analytics/rebuild` (body: `from`, `to`).
- **Admin playback events:** `GET /api/admin/playback-events` (filters, pagination).
- **Admin licenses:** `GET /api/admin/licenses`; `POST /api/admin/licenses/{licenseId}/revoke`.
- **Admin signed URLs:** `GET /api/admin/signed-urls` (filters, pagination).
