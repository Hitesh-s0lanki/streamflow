"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import Logo from "@/components/Logo";
import { ArrowLeft } from "lucide-react";

export function UploadHeader() {
  return (
    <header className="fixed top-0 left-0 right-0 z-50 border-b border-border/40 bg-background/80 backdrop-blur-xl supports-backdrop-filter:bg-background/60">
      <div className="flex items-center justify-between px-4 sm:px-6 lg:px-10 py-3.5 max-w-[1600px] mx-auto">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            asChild
            className="size-9 rounded-lg text-muted-foreground hover:text-foreground hover:bg-muted/80 transition-colors"
          >
            <Link href="/" aria-label="Back to home">
              <ArrowLeft className="size-5" />
            </Link>
          </Button>
          <Logo size="sm" />
        </div>
        <div className="w-24" />
      </div>
    </header>
  );
}
