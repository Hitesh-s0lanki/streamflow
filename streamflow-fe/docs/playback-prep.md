# Playback Preparation

## What Happens Before Playback Starts

Before the HLS/DASH player mounts and starts loading the manifest, the frontend must obtain a valid playback license and a short-lived signed manifest URL. Until both are ready, the playback page should show a preparation state (e.g. “Preparing playback…”) and not render the player or attempt to load the manifest.

---

## License Request Flow

1. **User context:** User must be signed in (Clerk). If not, redirect to sign-in or show “Sign in to watch” and do not call playback APIs.
2. **Request license:** Send `POST /api/playback/license` with body containing `videoAssetId` and optionally `deviceId`. Send header `X-User-Id` (Clerk userId). Optionally send `X-Device-Id` (e.g. stable device id for the browser).
3. **Response:** Backend returns a license object including `licenseId` and `expiresAt`. Store `licenseId` for the next step. If the backend returns 403 or 404 (e.g. asset not ready, user not allowed), do not proceed to manifest URL; show a user-friendly error (see below).
4. **Request manifest URL:** Send `POST /api/playback/{videoAssetId}/manifest-url` with body containing `licenseId`. Send `X-User-Id` again. Response includes the signed manifest URL (and possibly expiry). Use this URL as the source for the HLS/DASH player.
5. **Mount player:** Once the signed URL is available, mount the player component and set its source to that URL. The player will handle segment requests; if the backend requires signed segment URLs, the player or a custom loader will need to call `POST /api/playback/{videoAssetId}/segment-url` with `licenseId` and `segmentPath` for each segment, or the manifest may reference tokens that the backend has embedded—follow the actual API contract.

---

## Handling Expired Licenses

- **Before playback:** If the frontend has a cached license and it is past `expiresAt`, do not use it. Request a new license (`POST /api/playback/license`) and then a new manifest URL.
- **During playback:** If the player or app detects that playback is denied (e.g. 403 on segment or manifest refresh), treat the license as expired or revoked. Stop the player, clear the cached license/URL, and either prompt “Your session expired; please start playback again” and navigate back to content detail, or automatically request a new license and new manifest URL and resume if the product allows.
- **Validation (optional):** Before requesting the manifest URL, the frontend can call `GET /api/playback/license/{licenseId}` with `X-User-Id` to check that the license is still ACTIVE and not expired. If invalid, request a new license first.

---

## Fetching Signed Manifest URL

- **When:** Only after a valid license is available (new or validated).
- **Endpoint:** `POST /api/playback/{videoAssetId}/manifest-url` with body `{ "licenseId": "…" }` and header `X-User-Id`.
- **Use:** Pass the returned signed URL to the player as the manifest (HLS or DASH). Do not expose the URL in the UI or logs beyond what is necessary for debugging in development.
- **Expiry:** Signed URLs are short-lived. If playback will last longer than the URL’s validity, the backend may support refresh or the player may need to request a new manifest URL (and possibly a new license) when the current one fails; document the chosen strategy in the player implementation.

---

## Error Handling for Denied Playback

- **403 Forbidden:** User not allowed to play (e.g. content not available, license revoked). Show a clear message: “You don’t have access to play this” or “Playback is not available,” and do not retry automatically. Offer a link back to content detail or home.
- **404 Not Found:** Asset or license not found. Show “This title is unavailable” or “Playback link expired; please try again from the show page.” Optionally offer “Try again” that requests a new license.
- **409 or “asset not ready”:** Ingestion not READY. Show “This title is still processing; try again later” and link to content detail.
- **5xx / network:** Show “Something went wrong. Please try again.” With a retry button that re-runs the license + manifest flow once.

---

## Loading UI Before Player Mounts

- **State:** “Preparing playback…” (or similar) with a spinner or skeleton. No player visible, no manifest request from the player until the app has the signed URL.
- **Sequence:** (1) Check auth → (2) Request license → (3) Request manifest URL → (4) On success, mount player with URL; on failure, show error state.
- **Duration:** If license or manifest request hangs, show the same preparing state; consider a timeout (e.g. 15 s) after which show “Taking too long; please try again” with retry.

---

## Device and Session Handling

- **Device ID:** Optional `X-Device-Id` and/or body field `deviceId` can be sent with the license request. The frontend can derive a stable device id (e.g. stored in localStorage) so the backend can enforce per-device rules if needed. If not required by the product, it can be omitted.
- **Session:** One license per user per video asset (ACTIVE). If the user opens playback in another tab or device, behavior depends on backend policy (e.g. one active license per user per asset may invalidate the previous one). The frontend should handle 403 or validation failure by requesting a new license and optionally showing “Playback started elsewhere” if the product specifies it.
