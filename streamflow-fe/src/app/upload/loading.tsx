import { Skeleton } from "@/components/ui/skeleton";

export default function UploadLoading() {
  return (
    <div className="min-h-screen bg-background">
      <header className="fixed top-0 left-0 right-0 z-50 bg-background/95 backdrop-blur-md border-b border-border/50">
        <div className="flex items-center justify-between px-4 md:px-12 py-4">
          <Skeleton className="h-9 w-9 rounded-md" />
          <Skeleton className="h-6 w-32" />
          <div className="w-20" />
        </div>
      </header>
      <main className="pt-24 pb-12 px-4 md:px-12 max-w-6xl mx-auto space-y-8">
        <div className="flex gap-4">
          <Skeleton className="h-20 w-40 rounded-lg" />
          <Skeleton className="h-20 w-40 rounded-lg" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="space-y-6">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-[120px] w-full" />
            <div className="grid grid-cols-2 gap-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
            <Skeleton className="h-10 w-full" />
          </div>
          <div className="space-y-6">
            <Skeleton className="aspect-[2/3] max-w-[200px] rounded-lg" />
            <Skeleton className="aspect-video rounded-lg" />
            <Skeleton className="h-24 rounded-lg" />
          </div>
        </div>
      </main>
    </div>
  );
}
