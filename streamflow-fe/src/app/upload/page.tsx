"use client";

import { UploadHeader } from "./_components/UploadHeader";
import { UploadForm } from "./_components/UploadForm";

export default function UploadPage() {
  return (
    <div className="min-h-screen bg-linear-to-b from-muted/30 via-background to-background">
      <UploadHeader />
      <main className="pt-24 pb-20 px-4 sm:px-6 lg:px-10 max-w-5xl mx-auto">
        <div className="mb-10">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground">
            Upload content
          </h1>
          <p className="mt-1.5 text-muted-foreground text-sm sm:text-base max-w-xl">
            Add a new movie or series. Fill in the details, add artwork URLs, and upload your video file. Processing may take a few minutes.
          </p>
        </div>
        <UploadForm />
      </main>
    </div>
  );
}
