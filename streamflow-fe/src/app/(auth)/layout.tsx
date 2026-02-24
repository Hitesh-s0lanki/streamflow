import type { Metadata } from "next";
import Logo from "@/components/Logo";
import Features from "./_components/Features";

export const metadata: Metadata = {
  title: "Sign in — StreamFlow",
  description: "Sign in or sign up to StreamFlow.",
};

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="grid min-h-screen max-h-screen grid-cols-1 md:grid-cols-2 overflow-hidden">
      {/* Left: branding (dark) */}
      <aside className="flex flex-col justify-between bg-neutral-900 px-6 py-8 text-white md:px-10 md:py-12">
        <Logo size="sm" className="shrink-0 text-white" variant="dark" />
        <div className="hidden md:block">
          <h2 className="text-2xl font-semibold tracking-tight text-white md:text-3xl">
            Stream Smarter.
            <br />
            <span className="text-red-400">Securely.</span>
          </h2>
          <p className="mt-3 max-w-sm text-neutral-300">
            Your OTT platform for adaptive streaming, DRM protection, and
            reliable video delivery.
          </p>
          <Features />
        </div>
        <p className="text-sm text-neutral-400 md:text-base">
          StreamFlow — Stream Smarter. Securely.
        </p>
      </aside>

      {/* Right: auth form (white) */}
      <main className="flex flex-col items-center justify-center overflow-y-auto bg-background px-6 py-8 md:px-12 md:py-10">
        <div className="w-full max-w-[400px]">{children}</div>
      </main>
    </div>
  );
}
