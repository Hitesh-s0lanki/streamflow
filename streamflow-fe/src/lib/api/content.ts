import { apiFetch } from "@/lib/api/client";
import type { ContentCatalogItem, ContentDetail } from "@/types/content";

/** GET /api/content — catalog listing (PUBLISHED only, newest first). */
export async function getCatalog(): Promise<ContentCatalogItem[]> {
  return apiFetch<ContentCatalogItem[]>("/api/content");
}

/** GET /api/content/{contentId} — content detail for hero/detail page. */
export async function getContentDetail(contentId: string): Promise<ContentDetail> {
  return apiFetch<ContentDetail>(`/api/content/${contentId}`);
}
