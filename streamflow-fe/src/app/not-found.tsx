import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background px-4">
      <div className="text-center max-w-md">
        <p className="text-6xl md:text-8xl font-bold text-primary/20 tabular-nums select-none">
          404
        </p>
        <h1 className="mt-4 text-2xl font-semibold text-foreground">
          Page not found
        </h1>
        <p className="mt-2 text-muted-foreground">
          The page you’re looking for doesn’t exist or has been moved.
        </p>
        <Button asChild size="lg" className="mt-8">
          <Link href="/">Back to home</Link>
        </Button>
      </div>
    </div>
  );
}
