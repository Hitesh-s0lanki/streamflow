"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { getApiBaseUrl } from "@/lib/api/client";

export interface UploadProgressEvent {
  eventType: string;
  totalParts: number;
  uploadedParts: number;
  currentPartNumber: number;
  progressPercent: number;
  status: string;
}

export type UploadProgressEventHandler = (data: UploadProgressEvent) => void;

/**
 * Subscribes to upload progress via POST /api/demo/stream-event (SSE).
 * Use when the backend pushes progress for multipart uploads.
 * Reconnects when the stream ends (e.g. after server timeout).
 */
export function useUploadProgressStream(onEvent?: UploadProgressEventHandler) {
  const [event, setEvent] = useState<UploadProgressEvent | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const cancelledRef = useRef(false);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  const connect = useCallback(async () => {
    cancelledRef.current = false;
    setError(null);

    const base = getApiBaseUrl().replace(/\/$/, "");
    const url = `${base}/api/demo/stream-event`;

    try {
      const res = await fetch(url, {
        method: "POST",
        headers: { Accept: "text/event-stream" },
      });

      if (!res.ok || !res.body) {
        const err = new Error(res.ok ? "No response body" : `Stream failed: ${res.status}`);
        console.error("[SSE upload progress] Stream connection failed:", err.message, { status: res.status });
        throw err;
      }

      setIsConnected(true);
      const reader = res.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      try {
        while (!cancelledRef.current) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() ?? "";

          for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed.startsWith("data:") && trimmed.length > 5) {
              try {
                const json = trimmed.slice(5).trim();
                if (json) {
                  const data = JSON.parse(json) as UploadProgressEvent;
                  setEvent(data);
                  onEventRef.current?.(data);
                }
              } catch (e) {
                console.warn("[SSE upload progress] Invalid SSE JSON", e);
              }
            }
          }
        }
      } finally {
        reader.cancel().catch(() => {});
      }

      // Reconnect after stream ends (e.g. server timeout ~30s) unless cancelled
      if (!cancelledRef.current) {
        reconnectTimeoutRef.current = setTimeout(connect, 2000);
      }
    } catch (e) {
      if (!cancelledRef.current) {
        const err = e instanceof Error ? e : new Error(String(e));
        setError(err);
        console.error("[SSE upload progress] Error:", err);
        reconnectTimeoutRef.current = setTimeout(connect, 3000);
      }
    } finally {
      if (!cancelledRef.current) {
        setIsConnected(false);
      }
    }
  }, []);

  useEffect(() => {
    connect();
    return () => {
      cancelledRef.current = true;
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
    };
  }, [connect]);

  return { event, error, isConnected, reconnect: connect };
}
