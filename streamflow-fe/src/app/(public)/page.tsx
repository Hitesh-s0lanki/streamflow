"use client";

import { useMemo, useEffect, useRef } from "react";
import { useUser } from "@clerk/nextjs";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import Navbar from "@/components/Navbar";
import HeroBanner from "@/components/HeroBanner";
import ContentRow from "@/components/ContentRow";
import EmptyCatalog from "@/components/EmptyCatalog";
import ServerWakingUp from "@/app/(public)/_components/ServerWakingUp";
import { getCatalog, getContentDetail } from "@/lib/api/content";
import { getContinueWatching } from "@/lib/api/watch-progress";
import { useBackendHealth } from "@/hooks/use-backend-health";
import { Skeleton } from "@/components/ui/skeleton";

function HomeSkeleton() {
  return (
    <>
      <div className="relative h-[70vh] md:h-[80vh] w-full overflow-hidden -mt-[72px]">
        <Skeleton className="absolute inset-0 w-full h-full rounded-none" />
        <div className="absolute bottom-0 left-0 right-0 p-6 md:p-16 pb-20 md:pb-28 max-w-2xl">
          <Skeleton className="h-7 w-20 mb-4 rounded-full" />
          <Skeleton className="h-12 w-3/4 mb-3" />
          <Skeleton className="h-5 w-1/3 mb-4" />
          <Skeleton className="h-16 w-full mb-8" />
          <div className="flex gap-3">
            <Skeleton className="h-12 w-36 rounded-full" />
            <Skeleton className="h-12 w-36 rounded-full" />
          </div>
        </div>
      </div>

      <div className="px-4 md:px-12 py-8 space-y-10">
        {[1, 2].map((row) => (
          <div key={row}>
            <Skeleton className="h-6 w-44 mb-5" />
            <div className="flex gap-4">
              {Array.from({ length: 7 }).map((_, i) => (
                <div key={i} className="shrink-0 w-[150px] md:w-[185px]">
                  <Skeleton className="w-full aspect-2/3 rounded-xl" />
                  <Skeleton className="h-4 w-3/4 mt-2.5" />
                  <Skeleton className="h-3 w-1/2 mt-1" />
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </>
  );
}

export default function Home() {
  const { user, isSignedIn } = useUser();
  const { status: healthStatus, retry: healthRetry } = useBackendHealth();
  const prevHealthRef = useRef<typeof healthStatus>("unknown");

  const catalogQuery = useQuery({
    queryKey: ["content", "catalog"],
    queryFn: getCatalog,
  });

  const continueQuery = useQuery({
    queryKey: ["watch-progress", "continue"],
    queryFn: () => getContinueWatching(user!.id),
    enabled: isSignedIn === true && !!user?.id,
  });

  const firstId = catalogQuery.data?.[0]?.id;
  const featuredQuery = useQuery({
    queryKey: ["content", "detail", firstId],
    queryFn: () => getContentDetail(firstId!),
    enabled: !!firstId,
  });

  const catalog = useMemo(
    () => catalogQuery.data ?? [],
    [catalogQuery.data],
  );
  const continueWatching = continueQuery.data ?? [];
  const featured = featuredQuery.data;
  const isLoading = catalogQuery.isLoading;
  const isError = catalogQuery.isError;
  const errorMessage =
    catalogQuery.error instanceof Error
      ? catalogQuery.error.message
      : "Failed to load catalog.";

  const movies = useMemo(
    () => catalog.filter((c) => c.contentType === "MOVIE"),
    [catalog],
  );
  const series = useMemo(
    () => catalog.filter((c) => c.contentType === "SERIES"),
    [catalog],
  );

  const showWakingUp = isError && healthStatus !== "up";

  useEffect(() => {
    if (prevHealthRef.current !== "up" && healthStatus === "up") {
      prevHealthRef.current = "up";
      toast.success("Streamflow is ready", {
        description: "The catalog is loading.",
      });
      catalogQuery.refetch();
    } else {
      prevHealthRef.current = healthStatus;
    }
  }, [healthStatus, catalogQuery]);

  if (showWakingUp) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <Navbar />
        <ServerWakingUp
          isChecking={healthStatus === "unknown"}
          onRetry={() => {
            healthRetry();
            catalogQuery.refetch();
          }}
        />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
          <div className="rounded-2xl border border-border bg-card p-10 max-w-md shadow-sm">
            <p className="text-destructive font-semibold text-lg mb-2">
              Something went wrong
            </p>
            <p className="text-sm text-muted-foreground mb-6">{errorMessage}</p>
            <button
              type="button"
              onClick={() => catalogQuery.refetch()}
              className="px-6 py-2.5 rounded-full bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
            >
              Try Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (!isLoading && catalog.length === 0) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <EmptyCatalog />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />

      {isLoading ? (
        <HomeSkeleton />
      ) : (
        <>
          {featured ? (
            <HeroBanner content={featured} />
          ) : catalog.length > 0 ? (
            <div className="relative h-[40vh] min-h-[200px] flex items-center justify-center bg-linear-to-b from-muted/30 to-background">
              <p className="text-muted-foreground">No featured content</p>
            </div>
          ) : null}

          <div className="relative -mt-20 md:-mt-28 z-10 pb-20 space-y-1">
            {continueWatching.length > 0 && (
              <ContentRow
                title="Continue Watching"
                items={continueWatching}
                showProgress
              />
            )}

            {catalog.length > 0 && (
              <ContentRow title="Newly Added" items={catalog} />
            )}

            {movies.length > 0 && movies.length !== catalog.length && (
              <ContentRow title="Movies" items={movies} />
            )}

            {series.length > 0 && series.length !== catalog.length && (
              <ContentRow title="Series" items={series} />
            )}
          </div>
        </>
      )}
    </div>
  );
}
