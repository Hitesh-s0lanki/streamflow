# Frontend Architecture

## Overview

The Streamflow demo frontend is a Next.js application using the App Router. It provides a Netflix-like streaming UI that consumes existing backend APIs for catalog, ingestion, playback, and analytics. Authentication is handled by Clerk; the backend receives the user identity via headers where required.

---

## App Router Structure

- **Root layout:** Wraps the entire app; resolves auth (Clerk) and provides global providers (theme, API base URL, error boundary).
- **Route segments:** Each major section (home, content detail, playback, admin) is a segment under `app/`. Nested layouts define shared chrome (e.g. header, nav) for that section.
- **Pages:** Leaf route segments that render the main content for a URL. They are responsible for data fetching and composing UI; they do not contain business logic beyond orchestration.
- **Layouts:** Reusable wrappers that persist across navigations within a segment (e.g. main app layout with header/footer, admin layout with sidebar). Layouts should not fetch data that is specific to a single page.

---

## Separation of Concerns

### Pages

- Live under `app/` route segments.
- Define what data the route needs (e.g. catalog list, content detail, playback readiness).
- Call API client functions or data-fetching helpers; do not call backend URLs directly.
- Pass fetched or transformed data into presentational components.
- Handle route-level loading and error states (or delegate to shared patterns).

### UI Components

- Live under a dedicated components directory (e.g. `components/`).
- Are presentational: they receive props and render; they do not know backend shapes.
- Include: content cards, detail hero, season/episode lists, player shell, seek bar with preview, admin tables, skeletons, empty states, error messages.
- Reusable across pages and layouts.

### API Clients

- Live under a dedicated module (e.g. `lib/api/` or `services/api/`).
- One module (or file) per domain: content, ingestion, playback, watch-progress, analytics, admin.
- Expose functions that map to backend endpoints: e.g. `getCatalog()`, `getContentDetail(id)`, `requestLicense(videoAssetId)`, `getContinueWatching()`.
- Add required headers (e.g. `X-User-Id` from Clerk, `X-Device-Id` when implemented).
- Return typed responses or throw/handle errors in a consistent way so pages can react (e.g. 403, 404, 5xx).
- Do not contain UI logic.

### State Management

- **Server state:** Fetched data (catalog, detail, watch progress, continue watching) is treated as server state. Prefer server components and server-side fetching where possible; use client-side fetching (e.g. SWR, React Query, or plain fetch with caching) for client-only flows (e.g. playback, progress upsert).
- **Auth state:** Resolved via Clerk (e.g. `useUser()`, `userId`). Pass `userId` into API client calls or middleware that attach `X-User-Id`.
- **Local UI state:** Component-level state (e.g. selected season, modal open, player playing/paused) stays in React state or small context where needed; no global store required for simple UI toggles.
- **Playback state:** Player-specific state (current time, duration, buffering, license validity) can live in a playback context or in the player component tree so that progress upsert and event emission can access it.

---

## How the Frontend Talks to the Backend

- **Base URL:** Configured via environment variable (e.g. `NEXT_PUBLIC_API_URL`). All API client requests use this base.
- **REST:** Backend exposes REST endpoints. The frontend uses GET for reads and POST/PATCH for mutations. No backend internals (Kafka, ingestion pipeline details) are exposed; the frontend only uses the public API contract.
- **Auth:** For endpoints that require a user (playback license, watch progress, continue watching), the frontend sends `X-User-Id` (Clerk `userId`). Optional `X-Device-Id` can be sent for playback. Clerk session is not sent to the backend; only the derived user identifier is.
- **Errors:** Backend returns standard HTTP status codes and optionally JSON error bodies. The API client layer normalizes these into a simple contract (e.g. throw an error type or return a result object) so pages and components can show the right message or redirect.

---

## Where Auth Context Is Resolved

- **Clerk provider:** Wraps the app at the root so that `useAuth()` / `useUser()` are available wherever needed.
- **Middleware (optional):** Next.js middleware can protect routes (e.g. redirect unauthenticated users from `/play`, `/admin`) and optionally attach user info to the request.
- **API calls:** Before calling endpoints that require `X-User-Id`, the client reads the current user from Clerk (e.g. in a hook or inside the API client) and sets the header. If the user is not signed in and the endpoint requires it, the call should not be made or should fail gracefully with a clear “sign in to continue” message.

---

## Error Handling Strategy

- **API client:** Catches HTTP errors and maps them to a small set of error types or messages (e.g. not found, forbidden, server error, network error).
- **Pages:** Use error boundaries or try/catch around data fetching; on error, render a standard error view (see `error-and-loading.md`) with retry or navigation options.
- **Global error boundary:** Catches unhandled errors in the React tree and shows a generic “something went wrong” page with a way to go back or reload.
- **Form/action errors:** Inline validation and server error messages (e.g. from create content, confirm upload) are shown near the form or at the top of the step.

---

## Loading and Skeleton Strategy

- **Route-level loading:** Each route segment can expose a `loading.tsx` (or equivalent) that shows while the segment is loading; prefer skeletons that mirror the final layout (e.g. card grid skeleton for home, detail skeleton for content page).
- **Component-level loading:** For client-fetched data (e.g. continue watching row), show a compact skeleton or placeholder until data arrives.
- **Playback:** Before the player mounts, show a dedicated “Preparing playback…” state (see `playback-prep.md`); avoid flashing the player before the signed URL and license are ready.
- **Consistency:** Reuse a small set of skeleton components (card, list row, hero, text block) so the app feels consistent and predictable.
