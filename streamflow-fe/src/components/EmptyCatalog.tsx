"use client";

import { Film } from "lucide-react";
import {
  Empty,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
  EmptyDescription,
} from "@/components/ui/empty";

export default function EmptyCatalog() {
  return (
    <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center px-4 py-16">
      <Empty className="border-0 p-0 md:p-12">
        <EmptyMedia variant="icon" className="mb-4 size-16 [&_svg]:size-10">
          <Film className="text-muted-foreground" aria-hidden />
        </EmptyMedia>
        <EmptyHeader>
          <EmptyTitle className="text-xl font-semibold text-foreground">
            No content yet
          </EmptyTitle>
          <EmptyDescription className="mt-2 max-w-sm text-base">
            There are no movies or series in the catalog. Check back later or add
            content from the upload page.
          </EmptyDescription>
        </EmptyHeader>
      </Empty>
    </div>
  );
}
