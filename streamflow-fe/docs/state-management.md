# State Management

## What Global State Is Needed

- **Auth (Clerk):** The current user (userId, signed-in state) is global in the sense that it is provided by Clerk at the root. No custom global store is required for auth; use Clerk’s provider and hooks.
- **API base URL / config:** Environment-derived config (e.g. `NEXT_PUBLIC_API_URL`) can be read at build or runtime and passed into the API client; no reactive global state required unless the app supports runtime config switching.
- **Optional: playback context:** If the product needs “current playback” state (e.g. mini player, “now playing” bar) shared across routes, a small React context or store can hold: current videoAssetId, title, playing/paused, currentTime. This is optional; a single full-page player per route does not require global playback state.

---

## What Stays Local to Pages

- **Route/segment state:** Selected season, selected tab, modal open/closed, form values. Keep in component state or in URL (e.g. query params) where shareable.
- **List and detail data:** Catalog list, content detail, episode list, watch progress for a page. Fetched per route and cached as server or client cache (see below); not stored in a global store.
- **Playback page:** License id, signed manifest URL, player playing/paused, currentTime. Can live in the playback page component tree or a playback-specific context that is only mounted on the playback route. No need to lift to root unless a “mini player” or “now playing” bar is required.
- **Admin filters:** Pagination, filters (status, date range) on admin list pages. Keep in component state or URL so they persist on refresh if desired.

---

## How Auth State Is Accessed

- **Clerk:** Use Clerk’s React provider and hooks (e.g. `useUser()`, `useAuth()`). Read `userId` when making API calls that require `X-User-Id` (playback license, watch progress, continue watching). If the user is not signed in, do not call those endpoints; show sign-in prompt or redirect where appropriate.
- **Admin:** Admin status can be derived from Clerk (e.g. custom claim, role in user metadata) or from a separate check (e.g. allowlist). Resolve once per request or in layout and pass down or use in middleware to protect `/admin/*`.

---

## Caching Strategy

- **Server-rendered data:** For routes that can be server-rendered (e.g. home catalog, content detail), use Next.js server components and fetch on the server; Next will cache as per its defaults. Optionally use `revalidate` or cache tags for incremental revalidation.
- **Client-fetched data:** For data fetched on the client (e.g. continue watching, playback license, watch progress upsert), use a client-side cache (e.g. SWR, React Query, or a simple cache object) with a short stale time (e.g. 60 s for continue watching, no cache for license/manifest). Catalog and content detail can also be client-fetched with cache and revalidate on focus or interval if not server-rendered.
- **No long-lived cache for playback:** License and signed manifest URL should not be cached across sessions; treat them as short-lived and request fresh when entering the playback page.

---

## Revalidation Rules

- **Catalog:** Revalidate when the user returns to the home page (e.g. refetch on focus or use SWR/React Query revalidateOnFocus). Optionally revalidate after a time window (e.g. 5 minutes).
- **Content detail:** Revalidate on focus or when the user navigates back to the same content (e.g. after uploading a new episode). Invalidate or refetch after admin publish/unpublish if the user is on that content.
- **Continue watching:** Revalidate when the user lands on the home page and after they finish or leave playback (invalidate the continue-watching query or refetch).
- **Watch progress:** After upserting progress (e.g. from the player), optionally invalidate the continue-watching list and the content detail’s progress for that asset so the UI updates on next view.

---

## Polling vs Refetch Logic

- **Polling:** Use only where the backend does not push updates. Ingestion status during upload flow is polled (e.g. every 5–10 s) until READY or FAILED. Do not poll catalog or content detail; use refetch on focus or manual refresh.
- **Refetch:** Trigger refetch when the user takes an action that changes data (e.g. after confirm upload, after marking complete, after unpublish). Use “refetch on window focus” for lists (catalog, continue watching, admin lists) so returning to the tab shows fresh data.
- **No WebSocket in this spec:** If the backend does not expose WebSockets for ingestion or playback, the frontend relies on polling (ingestion) and one-time fetch (playback readiness).
