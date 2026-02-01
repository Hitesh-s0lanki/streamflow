import type { Metadata } from "next";
import { ClerkProvider } from "@clerk/nextjs";
import { Plus_Jakarta_Sans, JetBrains_Mono } from "next/font/google";
import { QueryProvider } from "./providers";
import "./globals.css";

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
      <html lang="en">
        <body
          className={`${plusJakartaSans.variable} ${jetbrainsMono.variable} font-sans antialiased`}
        >
          <QueryProvider>{children}</QueryProvider>
        </body>
      </html>
    </ClerkProvider>
  );
}
