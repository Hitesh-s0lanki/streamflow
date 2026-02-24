/**
 * API base URL for the Java backend. Must be NEXT_PUBLIC_ for client-side access.
 */
const getBaseUrl = (): string => {
  const url =
    typeof window !== "undefined"
      ? (process.env.NEXT_PUBLIC_JAVA_SERVER_URL ?? process.env.NEXT_PUBLIC_APP_URL)
      : process.env.JAVA_SERVER_URL ?? process.env.NEXT_PUBLIC_JAVA_SERVER_URL;
  if (!url) {
    throw new Error(
      "API base URL not configured. Set NEXT_PUBLIC_JAVA_SERVER_URL or JAVA_SERVER_URL in .env"
    );
  }
  return url.replace(/\/$/, "");
};

export function getApiBaseUrl(): string {
  return getBaseUrl();
}

export interface ApiErrorBody {
  message?: string;
  error?: string;
  status?: number;
}

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public body?: ApiErrorBody
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function parseErrorResponse(res: Response): Promise<ApiErrorBody> {
  const text = await res.text();
  try {
    return JSON.parse(text) as ApiErrorBody;
  } catch {
    return { message: text || res.statusText, error: res.statusText };
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const base = getBaseUrl();
  const url = `${base}${path.startsWith("/") ? path : `/${path}`}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
  });
  if (!res.ok) {
    const body = await parseErrorResponse(res);
    const message =
      body?.message ?? body?.error ?? `Request failed: ${res.status} ${res.statusText}`;
    throw new ApiError(message, res.status, body);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

/** PUT with binary body (e.g. file upload to presigned URL). */
export async function apiPutBinary(url: string, body: Blob): Promise<void> {
  const res = await fetch(url, {
    method: "PUT",
    body,
    headers: {
      "Content-Type": body.type || "application/octet-stream",
    },
  });
  if (!res.ok) {
    const text = await res.text();
    throw new ApiError(
      text || `Upload failed: ${res.status} ${res.statusText}`,
      res.status
    );
  }
}
