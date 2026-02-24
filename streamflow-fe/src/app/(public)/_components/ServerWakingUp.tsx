"use client";

import Link from "next/link";
import { CloudCog, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
} from "@/components/ui/empty";

interface ServerWakingUpProps {
  isChecking?: boolean;
  onRetry?: () => void;
}

export default function ServerWakingUp({
  isChecking = false,
  onRetry,
}: ServerWakingUpProps) {
  return (
    <div className="flex min-h-[calc(100vh-5rem)] flex-col items-center justify-center px-4 py-16">
      <div className="relative w-full max-w-md">
        <div
          className="absolute -inset-3 rounded-3xl bg-linear-to-br from-primary/10 via-transparent to-primary/5 opacity-80 blur-xl"
          aria-hidden
        />
        <Empty className="relative rounded-2xl border border-border bg-card/95 p-8 shadow-lg backdrop-blur sm:p-10 md:p-12">
          <EmptyMedia variant="icon" className="mb-5 size-16 [&_svg]:size-9">
            <CloudCog
              className="animate-pulse text-primary"
              strokeWidth={1.5}
              aria-hidden
            />
          </EmptyMedia>
          <EmptyHeader>
            <EmptyTitle className="text-xl font-semibold text-foreground sm:text-2xl">
              {isChecking ? "Checking server…" : "Streamflow is waking up"}
            </EmptyTitle>
            <EmptyDescription className="mt-3 max-w-sm text-base leading-relaxed">
              {isChecking
                ? "Verifying backend availability. This usually takes a moment."
                : "The server may be spinning up (e.g. on a free tier). This can take a minute. We’re checking every 10 seconds—you can explore the About page in the meantime."}
            </EmptyDescription>
          </EmptyHeader>
          <div className="mt-8 flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
            <Button
              variant="hero"
              size="default"
              className="w-full gap-2 rounded-full sm:w-auto"
              asChild
            >
              <Link href="/about">
                <Info className="size-4" aria-hidden />
                Explore About
              </Link>
            </Button>
            {onRetry && (
              <Button
                variant="secondary"
                size="default"
                className="w-full rounded-full sm:w-auto"
                onClick={onRetry}
              >
                Check again
              </Button>
            )}
          </div>
          <p className="mt-6 flex items-center justify-center gap-1.5 text-xs text-muted-foreground">
            <span
              className="inline-flex gap-1"
              aria-live="polite"
              aria-busy={isChecking}
            >
              <span className="size-1.5 animate-pulse rounded-full bg-muted-foreground/60" />
              {isChecking ? "Checking…" : "Next check in ~10s"}
            </span>
          </p>
        </Empty>
      </div>
    </div>
  );
}
