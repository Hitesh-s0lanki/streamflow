# Error and Loading

## Standard Error Patterns

- **API failure:** When an API call fails (4xx, 5xx, or network error), the API client layer normalizes the failure and either throws or returns an error result. The caller (page or component) handles it by showing a user-facing message and, where appropriate, a retry or navigation action.
- **Form validation:** For create/update forms (content, season, episode, upload confirm), show inline validation errors next to fields. For server-side validation errors (e.g. 400 with field errors), map them to the same inline pattern.
- **Route-level errors:** If the primary data for a route fails (e.g. content detail 404), render a full-page or section-level error state (see below). Do not leave a blank page or a broken layout.

---

## API Failure Handling

- **404 Not Found:** Content, asset, or license not found. Show “This title is unavailable” or “Page not found” with a link to home or catalog. Do not retry automatically.
- **403 Forbidden:** User not allowed (e.g. playback denied, admin required). Show “You don’t have access to this” or “Sign in required” with sign-in or home link. Do not retry without user action.
- **400 Bad Request:** Validation or business rule error. Show the message from the response body if present and safe to display; otherwise a generic “Invalid request. Please check your input.” Allow the user to correct and resubmit.
- **409 Conflict:** Conflicting state (e.g. duplicate, already processing). Show a short message explaining the conflict and what the user can do (e.g. “This is already being processed; check status below.”).
- **5xx Server Error:** Show “Something went wrong. Please try again.” With a “Try again” button that retries the request once or navigates back. Do not expose stack traces or internal details.
- **Network error (no response):** Treat like 5xx; show “Connection problem. Please check your network and try again.” With retry.

---

## Global Error Boundaries

- **Root error boundary:** Wrap the app (or the main layout) in an error boundary that catches unhandled React errors. Display a generic “Something went wrong” page with a “Go home” or “Reload” button. Log the error for debugging; do not render it to the user.
- **Segment error boundaries:** Optional: add error boundaries per route segment (e.g. playback, admin) so a failure in one segment does not take down the whole app. Each boundary shows a segment-specific message (e.g. “Playback error” with “Back to show”).
- **Recovery:** Error boundary UIs should offer a way to recover: navigate back, reload, or retry. Do not leave the user stuck.

---

## Skeleton Loaders

- **When:** Show skeletons while initial data is loading for a route or a major section (catalog grid, content detail, episode list, continue watching row, admin tables).
- **Shape:** Skeletons should mirror the final layout: card grid skeleton with placeholder cards; detail page skeleton with hero placeholder and list placeholders; table skeleton with rows and columns. Use a consistent style (e.g. shimmer or pulse).
- **Placement:** Use route-level `loading.tsx` (or equivalent) for the segment, or render skeletons inside the page until data arrives. Avoid layout shift: skeleton and final content should occupy similar space.
- **Playback:** Before the player mounts, use a dedicated “Preparing playback…” state (spinner or short message), not a video-shaped skeleton, to set the expectation that playback is initializing.

---

## Empty States

- **No catalog items:** Single message: “No titles available” or “Nothing in the catalog yet.” Optional CTA for admin to add content.
- **No continue watching:** Do not show the “Continue Watching” section; no message needed.
- **No search/filter results:** When the user applies filters (e.g. on admin content list) and the result set is empty, show “No results match your filters.” With an option to clear filters or adjust.
- **No ingestion jobs:** On admin ingestion list, show “No ingestion jobs” or “No jobs match your filters.”

---

## User-Friendly Error Messaging

- **Tone:** Clear and concise. Avoid technical terms (e.g. “403,” “licenseId,” “videoAssetId”) unless in a debug panel.
- **Action:** Every error state should suggest a next step: “Try again,” “Go back,” “Sign in,” “Check your connection,” “Contact support” (if applicable).
- **Consistency:** Use the same phrasing for the same situation across the app (e.g. “This title is unavailable” for 404 on content/playback).
- **Localization:** If the app will be localized, keep messages in a single place (e.g. copy file or i18n keys) so they can be translated later.
