import Link from "next/link";
import { Button } from "@/components/ui/button";
import Logo from "@/components/Logo";

export default function Header() {
  return (
    <header className="relative z-10 flex items-center justify-between px-4 py-4 md:px-6 md:py-5">
      <Logo size="sm" />
      <Link href="/login">
        <Button variant="hero" size="default" className="rounded-full px-5">
          Sign In
        </Button>
      </Link>
    </header>
  );
}
