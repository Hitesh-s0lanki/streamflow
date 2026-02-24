import { getApiBaseUrl } from "./client";

/** Response shape from GET /api/health (backend). */
export interface HealthResponse {
  status: "UP" | "DOWN";
  version?: string;
  timestamp?: string;
  components?: { database?: string };
}

const HEALTH_PATH = "/api/health";
const DEFAULT_TIMEOUT_MS = 6_000;

export interface CheckHealthResult {
  ok: boolean;
  status?: number;
  body?: HealthResponse;
  error?: string;
}

/**
 * Calls GET /api/health with a timeout. Does not throw.
 * Use for liveness/readiness; 200 + body.status === "UP" means healthy.
 */
export async function checkHealth(
  timeoutMs: number = DEFAULT_TIMEOUT_MS
): Promise<CheckHealthResult> {
  let base: string;
  try {
    base = getApiBaseUrl();
  } catch {
    return { ok: false, error: "API URL not configured" };
  }

  const url = `${base}${HEALTH_PATH}`;
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const res = await fetch(url, {
      method: "GET",
      signal: controller.signal,
      headers: { Accept: "application/json" },
    });
    clearTimeout(id);
    const body = (await res.json().catch(() => undefined)) as
      | HealthResponse
      | undefined;
    const ok = res.ok && body?.status === "UP";
    return { ok, status: res.status, body };
  } catch (e) {
    clearTimeout(id);
    const message = e instanceof Error ? e.message : "Network error";
    return { ok: false, error: message };
  }
}
