import { SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import Link from "next/link";
import { Upload, Github } from "lucide-react";
import { Button } from "@/components/ui/button";
import Logo from "@/components/Logo";

const GITHUB_URL = "https://github.com/Hitesh-s0lanki/streamflow";

const navLinkClass =
  "text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-md px-2 py-1.5";

export default function Navbar() {
  return (
    <header className="sticky top-0 z-50 w-full">
      <div className="mx-auto flex h-16 max-w-[1600px] items-center justify-between gap-4 px-4 md:px-6 lg:px-8">
        <Logo size="sm" />

        <div className="flex items-center gap-1 sm:gap-2">
          <SignedOut>
            <nav className="flex items-center gap-1 sm:gap-2" aria-label="Main">
              <Link href="/about" className={navLinkClass}>
                About
              </Link>
              <a
                href={GITHUB_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={`${navLinkClass} flex items-center justify-center p-1.5`}
                aria-label="View on GitHub"
              >
                <Github className="size-5" aria-hidden />
              </a>
            </nav>
            <div className="ml-2 flex items-center gap-2 border-l border-border/60 pl-3">
              <Link href="/sign-in">
                <Button
                  variant="hero"
                  size="default"
                  className="rounded-full px-4 py-2 text-sm font-semibold sm:px-5"
                >
                  Sign In
                </Button>
              </Link>
              <Link href="/sign-up">
                <Button
                  variant="outline"
                  size="default"
                  className="rounded-full border-border/80 px-4 py-2 text-sm font-medium sm:px-5"
                >
                  Sign Up
                </Button>
              </Link>
            </div>
          </SignedOut>

          <SignedIn>
            <Link href="/upload">
              <Button
                variant="ghost"
                size="sm"
                className="gap-2 rounded-full px-3 py-2 text-sm font-medium hover:bg-accent"
              >
                <Upload className="size-4" aria-hidden />
                Upload
              </Button>
            </Link>
            <nav
              className="flex items-center gap-1 sm:gap-2"
              aria-label="Secondary"
            >
              <Link href="/about" className={navLinkClass}>
                About
              </Link>
              <a
                href={GITHUB_URL}
                target="_blank"
                rel="noopener noreferrer"
                className={`${navLinkClass} flex items-center justify-center p-1.5`}
                aria-label="View on GitHub"
              >
                <Github className="size-5" aria-hidden />
              </a>
            </nav>
            <div className="ml-1 flex items-center border-l border-border/60 pl-2">
              <UserButton
                afterSignOutUrl="/"
                appearance={{
                  elements: {
                    avatarBox: "size-8 sm:size-9 ring-0",
                  },
                }}
              />
            </div>
          </SignedIn>
        </div>
      </div>
    </header>
  );
}
