# Catalog Pages

## Home Page Behavior

- **Purpose:** Entry point: show the main catalog (published content) and, when the user is signed in, a “Continue Watching” row.
- **Data:** `GET /api/content` returns the public catalog (PUBLISHED only, e.g. ordered by createdAt DESC). If signed in, `GET /api/watch-progress/continue` returns in-progress items for the continue-watching row.
- **Layout:** Hero or featured area (optional) plus a catalog grid of content cards. Continue watching row appears above or within the catalog when there is data and the user is signed in.
- **Empty catalog:** Show a single empty state message (e.g. “No titles yet”) and, for admins, a link to the upload flow.

---

## Catalog Grid Layout Logic

- **Source:** The list returned by `GET /api/content`. No client-side filtering by publish status; the backend returns only PUBLISHED items for this endpoint.
- **Order:** Use the order provided by the API (e.g. newest first). If the API supports sort parameters in the future, respect them; otherwise do not re-sort on the client for the public catalog.
- **Grid:** Responsive grid (e.g. 2–4 columns on small screens, more on large). Each item is a content card linking to `/content/[contentId]`.
- **Density:** Fixed or minimum card size so the grid stays readable; avoid tiny cards on large screens.

---

## Loading States

- **Initial load:** Show a skeleton grid that matches the card layout (e.g. same number of placeholder cards or rows). No content until the catalog response is received.
- **Continue watching:** While loading, show a short skeleton row (e.g. horizontal strip of placeholders) or hide the row until data is available. Do not block the rest of the page.

---

## Empty States

- **No catalog items:** Single clear message: “No titles available” or “Nothing in the catalog yet.” Optionally show an admin CTA to add content.
- **No continue watching:** Do not show the “Continue Watching” section at all when the list is empty; no empty-state message needed for that row.

---

## Content Card Behavior

- **Click / tap:** Navigate to `/content/[contentId]` (content detail page). No play from card unless product explicitly requires it (e.g. “Play” on hover); if so, use the same rules as content detail for which video asset to play (movie vs first episode).
- **Hover (desktop):** Optional: scale or highlight; optional short preview (e.g. title, rating). Do not auto-play video on hover unless specified; if implemented, use a small preview asset and respect bandwidth.
- **Focus:** Keyboard-accessible; Enter/Space activates the same action as click.

---

## Fields Displayed on Cards

- **Required:** Title; poster or thumbnail image (posterUrl or thumbnailUrl from content).
- **Optional:** Release year, rating, duration (if provided by the API). Prefer backend-provided fields; do not compute duration on the frontend from episodes unless the API does not provide it.
- **No backend internals:** Do not show ingestion status, contentId, or draft state on the public catalog; the public catalog is PUBLISHED only.

---

## Sorting and Filtering Rules

- **Public catalog:** Use default order from `GET /api/content`. If the backend later adds query params (e.g. sort=newest, genre), the frontend can expose sort/filter controls and pass them through. Until then, no client-side sort/filter is required.
- **Admin content list:** Uses `GET /api/admin/content` with optional `publishStatus`, `contentType`, `title`, `page`, `size`. Admin can filter by draft/published, movie/series, and search by title; see admin-panels.md.

---

## Pagination or Infinite Scroll

- **Public catalog:** If the API returns a finite list (no pagination), show all items. If the API supports pagination (page/size or cursor), implement either “Load more” / infinite scroll or page controls, and request the next page when the user triggers it. Document the chosen pattern in the implementation (e.g. “infinite scroll with 20 items per request”).
- **Continue watching:** Single list from `GET /api/watch-progress/continue`; no pagination in the current API. Show all returned items (e.g. last 30 days, in-progress only).

---

## Excluding Unpublished Content

- **Public catalog:** The `GET /api/content` endpoint returns only PUBLISHED content. The frontend does not need to filter; simply render what is returned.
- **Direct link to content:** `GET /api/content/{contentId}` returns detail for both PUBLISHED and DRAFT when accessed by ID (e.g. for preview or admin). The home catalog never links to DRAFT because DRAFT items are not in the catalog response. If the product requires hiding detail for DRAFT for non-admin users, the frontend can check publishStatus and show “Not available” or redirect; otherwise, rely on the backend to enforce visibility where needed.
