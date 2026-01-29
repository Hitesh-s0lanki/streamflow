import Link from "next/link";
import { Button } from "@/components/ui/button";
import Features from "./Features";

export default function Hero() {
  return (
    <main className="relative z-10 flex-1 flex flex-col items-center justify-center px-4 py-8 text-center">
      <div className="max-w-2xl mx-auto animate-fade-in">
        <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-4 leading-tight tracking-tight">
          Stream Smarter.
          <br />
          <span className="text-gradient">Securely.</span>
        </h1>

        <p className="text-base md:text-lg text-muted-foreground mb-6 max-w-md mx-auto leading-relaxed">
          Demo OTT platform with Netflix-style streaming and secure video delivery.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link href="/login">
            <Button variant="hero" size="lg" className="animate-pulse-glow rounded-full px-7">
              Get Started
            </Button>
          </Link>
          <Link href="/login">
            <Button variant="heroSecondary" size="lg" className="rounded-full px-7">
              Sign In
            </Button>
          </Link>
        </div>

        <Features />
      </div>
    </main>
  );
}
