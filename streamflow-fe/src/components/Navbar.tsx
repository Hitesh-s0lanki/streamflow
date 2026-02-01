import { SignedIn, SignedOut, UserButton } from "@clerk/nextjs";
import Link from "next/link";
import { Upload } from "lucide-react";
import { Button } from "@/components/ui/button";
import Logo from "@/components/Logo";

export default function Navbar() {
  return (
    <header className="relative z-10 flex items-center justify-between px-4 py-4 md:px-6 md:py-5">
      <Logo size="sm" />
      <div className="flex items-center gap-3">
        <SignedOut>
          <Link href="/sign-in">
            <Button variant="hero" size="default" className="rounded-full px-5">
              Sign In
            </Button>
          </Link>
          <Link href="/sign-up">
            <Button
              variant="secondary"
              size="default"
              className="rounded-full px-5"
            >
              Sign Up
            </Button>
          </Link>
        </SignedOut>
        <SignedIn>
          <Link href="/upload">
            <Button
              variant="secondary"
              size="default"
              className="rounded-full gap-2 px-4"
            >
              <Upload className="size-4" aria-hidden />
              Upload
            </Button>
          </Link>
          <UserButton afterSignOutUrl="/" />
        </SignedIn>
      </div>
    </header>
  );
}
