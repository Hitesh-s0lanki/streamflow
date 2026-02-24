"use client";

import { useState, useEffect, useRef, useCallback } from "react";
import { checkHealth } from "@/lib/api/health";

const POLL_INTERVAL_MS = 10_000;
const FIRST_CHECK_TIMEOUT_MS = 5_000;

export type BackendHealthStatus = "unknown" | "up" | "down";

export function useBackendHealth() {
  const [status, setStatus] = useState<BackendHealthStatus>("unknown");
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const checkingRef = useRef(false);

  const runCheck = useCallback(async () => {
    if (checkingRef.current) return;
    checkingRef.current = true;
    const result = await checkHealth(FIRST_CHECK_TIMEOUT_MS);
    checkingRef.current = false;
    if (result.ok) {
      setStatus("up");
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    } else {
      setStatus("down");
    }
  }, []);

  useEffect(() => {
    runCheck();
  }, [runCheck]);

  useEffect(() => {
    if (status !== "down") return;
    intervalRef.current = setInterval(runCheck, POLL_INTERVAL_MS);
    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [status, runCheck]);

  return { status, retry: runCheck };
}
