# Upload Flow (Admin)

## End-to-End Admin Upload Flow

The admin upload flow takes content from “no content” to “playable in the app.” It is a linear sequence of steps; the UI guides the admin through each step and reflects success and failure clearly.

---

## Steps

### 1. Create Content

- **Action:** Admin submits a form with title, description, contentType (MOVIE or SERIES), optional releaseYear, rating, posterUrl, thumbnailUrl.
- **Backend:** `POST /api/content` with a create-content payload. Response returns the created content (e.g. contentId, publishStatus DRAFT).
- **UI state:** Form valid; on success, store contentId and move to the next step. On error, show validation or server error message and keep the user on the form.

### 2. Create Season and Episode (if SERIES)

- **Condition:** Only when contentType is SERIES.
- **Season:** Admin creates one or more seasons (e.g. “Season 1”). Backend: `POST /api/content/{contentId}/seasons` with seasonNumber and optional title, posterUrl. Store returned seasonId(s).
- **Episodes:** For each season, admin creates episodes. Backend: `POST /api/seasons/{seasonId}/episodes` with episodeNumber, title, description, durationSeconds, thumbnailUrl. Each created episode has a videoAssetId (created by the backend). For MOVIE, skip this step; the backend will have created a single video asset for the content.

### 3. Create Video Asset

- **MOVIE:** A single video asset is typically created with the content (or via an explicit “create video asset” step if the API requires it). Confirm with backend contract: either content create returns a videoAssetId or a separate `POST /api/video-assets` is used with contentId (and no episodeId).
- **SERIES:** Each episode creation returns a videoAssetId. No separate “create video asset” step for episodes.
- **Backend:** When explicit, `POST /api/video-assets` with contentId (movie) or contentId + episodeId (episode). Response includes videoAssetId.
- **UI state:** After this step, the frontend has at least one videoAssetId to use for upload.

### 4. Request S3 Upload URL

- **Action:** For the chosen videoAssetId, the frontend requests a presigned PUT URL.
- **Backend:** `POST /api/video-assets/{videoAssetId}/upload-url`. Response includes the presigned URL and the raw S3 key (or equivalent) needed for confirm.
- **UI state:** Store presigned URL and raw key; show “Choose file” and “Upload” controls. Do not expose the raw URL in the UI for long; use it only for the upload request.

### 5. Upload File from Browser

- **Action:** User selects a file; frontend uploads the file with a PUT request to the presigned URL (binary body). Use the same file for the request (no transformation).
- **UI state:** Show progress (e.g. indeterminate or percent if the environment supports it). On network error, allow retry (re-request upload URL if the backend allows, then re-upload). On success (HTTP 200), proceed to confirm.

### 6. Confirm Upload

- **Action:** Frontend tells the backend that the upload is complete and provides the raw S3 key so the backend can create an ingestion job and start processing.
- **Backend:** `POST /api/ingestion/{videoAssetId}/uploaded` with body containing the raw key (e.g. rawS3Key). Backend creates/updates ingestion job and typically emits to Kafka for pipeline processing.
- **UI state:** On success, show “Processing started” and move to the ingestion status step. On error (e.g. invalid key, conflict), show message and allow retry or re-upload.

### 7. Track Ingestion Status

- **Action:** Frontend polls or refreshes ingestion status for the video asset.
- **Backend:** `GET /api/ingestion/{videoAssetId}` returns current job status (e.g. PENDING, UPLOADING, UPLOADED, PROCESSING, TRANSCODED, SPRITES_GENERATED, READY, FAILED) and optional errorMessage.
- **UI state:** Show current status and, if FAILED, the error message. Until READY, keep polling at a reasonable interval (e.g. every 5–10 seconds). When READY, show “Ready for playback” and optionally link to content detail or playback. Stop polling when READY or FAILED.

---

## UI States per Step

- **Idle:** Form or action available; no loading.
- **Submitting:** Request in flight; disable submit, show spinner or inline loading.
- **Success:** Show success message and, where defined, advance to the next step or show next section.
- **Error:** Show inline or toast error with message; keep user on the same step and allow retry or correction.
- **Upload progress:** During file upload, show progress indicator; on failure, allow “Retry upload” (and re-fetch upload URL if needed).
- **Ingestion polling:** Show “Processing…” with current status; on READY/FAILED, stop and show final state.

---

## Error States and Retry Strategy

- **Create content / season / episode / video asset:** On 4xx/5xx, show error message; user can fix input and resubmit. No automatic retry.
- **Upload URL:** On failure, show “Could not get upload link”; offer “Try again” (one more request). Do not retry indefinitely.
- **File upload:** On network or 5xx, show “Upload failed”; offer “Retry.” Optionally allow re-requesting upload URL and uploading again.
- **Confirm upload:** On 400/409 or 5xx, show error (e.g. “Invalid file” or “Already processing”); allow retry or re-upload depending on backend semantics.
- **Ingestion status:** On poll failure, retry the poll a few times with backoff; if repeatedly failing, show “Could not refresh status” with manual “Refresh” button.

---

## What the UI Polls vs Event-Driven

- **Polling:** Ingestion status (`GET /api/ingestion/{videoAssetId}`) is polled by the frontend at an interval (e.g. 5–10 s) until status is READY or FAILED. The backend does not push status to the client.
- **Event-driven:** The backend may use Kafka internally to drive the pipeline; the frontend does not subscribe to Kafka. From the frontend’s perspective, the only way to know when ingestion is done is to poll the ingestion endpoint or to have the user refresh.

---

## Demo Shortcuts for Admin

- **Skip transcoding (if supported):** If the backend supports an admin override to set ingestion job status (e.g. mark as READY for demo), the admin panel can offer a “Mark as ready (demo)” control that calls `POST /api/admin/ingestion/{jobId}/status` with READY. Use only in non-production or demo environments.
- **Pre-filled metadata:** Admin upload page can pre-fill sample title, description, and season/episode names to speed up demos.
- **Single-page wizard:** The entire flow can be a single multi-step wizard (steps 1–7) so the admin does not leave the page until the asset is READY or FAILED; this reduces context switching and makes demos clearer.
