"use client";

import { useUser } from "@clerk/nextjs";
import { useQuery } from "@tanstack/react-query";
import Navbar from "@/components/Navbar";
import HeroBanner from "@/components/HeroBanner";
import ContentRow from "@/components/ContentRow";
import EmptyCatalog from "@/components/EmptyCatalog";
import { getCatalog, getContentDetail } from "@/lib/api/content";
import { getContinueWatching } from "@/lib/api/watch-progress";
import { Skeleton } from "@/components/ui/skeleton";

export default function Home() {
  const { user, isSignedIn } = useUser();

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

  const catalog = catalogQuery.data ?? [];
  const continueWatching = continueQuery.data ?? [];
  const featured = featuredQuery.data;
  const isLoading = catalogQuery.isLoading;
  const isError = catalogQuery.isError;
  const errorMessage =
    catalogQuery.error instanceof Error
      ? catalogQuery.error.message
      : "Failed to load catalog.";

  if (isError) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <Navbar />
        <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
          <p className="text-destructive font-medium mb-2">
            Failed to load catalog. Please try again.
          </p>
          <p className="text-sm text-muted-foreground mb-4 max-w-md">
            {errorMessage}
          </p>
          <button
            type="button"
            onClick={() => catalogQuery.refetch()}
            className="px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  const hasNoContent = !isLoading && catalog.length === 0;

  if (hasNoContent) {
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
        <div className="relative h-[80vh] md:h-[90vh] w-full overflow-hidden">
          <Skeleton className="absolute inset-0 w-full h-full rounded-none" />
          <div className="absolute bottom-0 left-0 right-0 p-4 md:p-12 pb-24 md:pb-32 max-w-2xl">
            <Skeleton className="h-12 w-3/4 mb-4" />
            <Skeleton className="h-5 w-1/2 mb-4" />
            <Skeleton className="h-20 w-full mb-6" />
            <div className="flex gap-4">
              <Skeleton className="h-10 w-24" />
              <Skeleton className="h-10 w-28" />
            </div>
          </div>
        </div>
      ) : featured ? (
        <HeroBanner content={featured} />
      ) : catalog.length > 0 ? (
        <div className="relative h-[40vh] min-h-[200px] flex items-center justify-center">
          <p className="text-muted-foreground">No featured content</p>
        </div>
      ) : null}

      <div className="relative -mt-32 md:-mt-40 z-10 pb-16">
        {!isLoading && continueWatching.length > 0 && (
          <ContentRow
            title="Continue Watching"
            items={continueWatching}
            showProgress
          />
        )}

        {!isLoading && catalog.length > 0 && (
          <ContentRow title="New Releases" items={catalog} />
        )}
      </div>
    </div>
  );
}
