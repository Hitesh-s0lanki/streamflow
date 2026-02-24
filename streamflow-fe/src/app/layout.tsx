import type { Metadata } from "next";
import { ClerkProvider } from "@clerk/nextjs";
import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import { QueryProvider } from "./providers";
import "./globals.css";
import { Toaster } from "sonner";
import { TRPCReactProvider } from "@/trpc/client";
import { Analytics } from "@vercel/analytics/next";

const plusJakartaSans = Plus_Jakarta_Sans({
  variable: "--font-ott-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
});

const jetbrainsMono = JetBrains_Mono({
  variable: "--font-ott-mono",
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  display: "swap",
});

export const viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 5,
};

export const metadata: Metadata = {
  title: "StreamFlow — Stream Smarter. Securely.",
  description:
    "Demo OTT platform showcasing Netflix-style streaming with HLS/DASH, DRM protection, and secure video delivery.",
  keywords: ["OTT", "streaming", "video", "HLS", "DASH", "DRM", "demo"],
  openGraph: {
    title: "StreamFlow — Stream Smarter. Securely.",
    description:
      "Demo OTT platform with adaptive streaming, DRM protection, and preview thumbnails.",
  },
  icons: {
    icon: "/logo.png",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <ClerkProvider
      signInFallbackRedirectUrl="/"
      signUpFallbackRedirectUrl="/"
      signInForceRedirectUrl="/"
      signUpForceRedirectUrl="/"
    >
      <TRPCReactProvider>
        <html lang="en" className="min-w-0">
          <body
            className={`${plusJakartaSans.variable} ${jetbrainsMono.variable} font-sans antialiased min-w-0`}
          >
            <QueryProvider>
              <Toaster />
              {children}
            </QueryProvider>
            <Analytics />
          </body>
        </html>
      </TRPCReactProvider>
    </ClerkProvider>
  );
}
