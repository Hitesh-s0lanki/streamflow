"use client";

import Navbar from "@/components/Navbar";
import { motion } from "framer-motion";
import {
  Zap,
  Radio,
  Image as ImageIcon,
  Cloud,
  Shield,
  Sparkles,
  Film,
  Cog,
  Lightbulb,
  Rocket,
  Quote,
  ExternalLink,
} from "lucide-react";
import Link from "next/link";
import { SpriteSheetDemo } from "./_components/SpriteSheetDemo";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

const fadeInUp = {
  initial: { opacity: 0, y: 20 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: "-60px" },
  transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] as const },
};

const stagger = (i: number) => ({ delay: i * 0.08 });

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="mx-auto max-w-4xl px-4 pb-24 pt-6 md:px-6 md:pt-10">
        {/* Hero */}
        <motion.header
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-center pb-12 md:pb-16"
        >
          <span className="inline-flex items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-medium text-primary mb-6">
            <Film className="size-3.5" />
            OTT streaming platform
          </span>
          <h1 className="text-4xl font-bold tracking-tight md:text-5xl lg:text-6xl bg-linear-to-r from-foreground to-foreground/80 bg-clip-text text-transparent">
            Streamflow
          </h1>
          <p className="mt-4 text-lg text-muted-foreground max-w-xl mx-auto">
            Building an OTT platform from curiosity, not just code.
          </p>
        </motion.header>

        {/* It started with a question */}
        <motion.section {...fadeInUp} className="space-y-6">
          <h2 className="text-2xl font-semibold flex items-center gap-2">
            <Lightbulb className="size-6 text-primary" />
            It started with a simple question…
          </h2>
          <p className="text-muted-foreground leading-relaxed">
            While scrolling Instagram one day, I saw a short reel explaining how{" "}
            <strong className="text-foreground">sprite sheets</strong> work in
            OTT platforms. It showed how Netflix-style video previews appear
            instantly when you hover on the timeline. That one concept made me
            pause.
          </p>
          <p className="text-foreground font-medium">Then I asked myself:</p>
          <ul className="list-disc list-inside space-y-2 text-muted-foreground pl-2">
            {[
              "How does that actually work?",
              "How does the video automatically adjust quality?",
              "How do platforms stream smoothly even on slow internet?",
              "What happens behind the scenes after you upload a video?",
            ].map((q, i) => (
              <motion.li
                key={q}
                {...fadeInUp}
                transition={{
                  ...fadeInUp.transition,
                  delay: stagger(i as number).delay,
                }}
              >
                {q}
              </motion.li>
            ))}
          </ul>
          <p className="text-muted-foreground leading-relaxed">
            Instead of just searching answers… I built{" "}
            <strong className="text-foreground">Streamflow</strong>.
          </p>
        </motion.section>

        {/* Sprite sheet example - interactive (early so readers see it right after the intro) */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold mb-2">
            Example of a sprite sheet grid
          </h2>
          <p className="text-muted-foreground mb-8">
            Below is an example of how sprite sheets are structured. Each small
            tile is a frame extracted at a specific interval. When you hover
            over the timeline, the player calculates time position, frame index,
            grid coordinates, and displays the correct section — all in
            milliseconds.
          </p>
          <SpriteSheetDemo />
        </motion.section>

        {/* What is Streamflow */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold flex items-center gap-2 mb-2">
            <Rocket className="size-6 text-primary" />
            What is Streamflow?
          </h2>
          <p className="text-muted-foreground leading-relaxed mb-6">
            <strong className="text-foreground">Streamflow</strong> is a
            full-stack OTT streaming platform built to deeply understand how
            modern video platforms work. It&apos;s not just about playing
            videos. It&apos;s about:
          </p>
          <ul className="grid gap-3 sm:grid-cols-2">
            {[
              { icon: Zap, label: "Performance optimization" },
              { icon: Radio, label: "Adaptive streaming" },
              {
                icon: ImageIcon,
                label: "Smart preview thumbnails (Sprite Sheets)",
              },
              { icon: Cloud, label: "Cloud-based media handling" },
              { icon: Shield, label: "Secure content delivery" },
            ].map(({ icon: Icon, label }, i) => (
              <motion.li
                key={label}
                {...fadeInUp}
                transition={{ delay: stagger(i).delay }}
                className="flex items-center gap-3 rounded-lg border border-border/60 bg-card px-4 py-3 text-sm"
              >
                <Icon className="size-4 text-primary shrink-0" />
                <span>{label}</span>
              </motion.li>
            ))}
          </ul>
        </motion.section>

        {/* Core features */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold flex items-center gap-2 mb-8">
            <Sparkles className="size-6 text-primary" />
            Core features
          </h2>

          <div className="space-y-12">
            {/* HLS */}
            <Card className="border-border/60 overflow-hidden">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/15 text-primary font-semibold">
                    1
                  </span>
                  <CardTitle className="text-xl">
                    Adaptive streaming (HLS)
                  </CardTitle>
                </div>
                <CardDescription className="text-base italic">
                  Smooth playback. No buffering stress.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-muted-foreground">
                  Streamflow uses{" "}
                  <strong className="text-foreground">
                    HLS (HTTP Live Streaming)
                  </strong>{" "}
                  with automatic quality adjustment. When you watch a video: if
                  your internet is fast you get HD; if it slows down it switches
                  to lower quality; when it improves, quality goes back up. All
                  without you noticing.
                </p>
                <ul className="grid gap-2 text-sm text-muted-foreground">
                  {[
                    "No buffering interruptions",
                    "Optimized bandwidth usage",
                    "Better mobile experience",
                    "Real-world OTT behavior",
                  ].map((item) => (
                    <li key={item} className="flex items-center gap-2">
                      <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                      {item}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>

            {/* Sprite sheets */}
            <Card className="border-border/60 overflow-hidden">
              <CardHeader>
                <div className="flex items-center gap-2">
                  <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/15 text-primary font-semibold">
                    2
                  </span>
                  <CardTitle className="text-xl">
                    Sprite sheets (the feature that started it all)
                  </CardTitle>
                </div>
                <CardDescription className="text-base">
                  When you hover over a video timeline and see preview
                  thumbnails instantly — that&apos;s sprite sheet technology.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <p className="text-muted-foreground">
                  Instead of loading many small images: one large image is
                  generated with multiple frames in a grid, and the player shows
                  the correct frame instantly.
                </p>
                <ul className="grid gap-2 text-sm text-muted-foreground">
                  {[
                    "Faster preview loading",
                    "Fewer server requests",
                    "Reduced bandwidth usage",
                    "Better user experience",
                  ].map((item) => (
                    <li key={item} className="flex items-center gap-2">
                      <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                      {item}
                    </li>
                  ))}
                </ul>
              </CardContent>
            </Card>
          </div>
        </motion.section>

        {/* YouTube */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold flex items-center gap-2 mb-4">
            <Film className="size-6 text-primary" />
            The video that inspired this
          </h2>
          <p className="text-muted-foreground mb-6">
            To understand sprite sheets deeper, I explored technical breakdowns
            that led into frame extraction pipelines, background video
            processing, image stitching, metadata handling, and distributed
            processing systems — and eventually into building my own
            architecture.
          </p>
          <Link
            href="https://www.youtube.com/watch?v=-JtjQ-OA7XE"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex"
          >
            <Button variant="outline" size="lg" className="gap-2 rounded-full">
              <ExternalLink className="size-4" />
              YouTube: Sprite sheets & OTT concepts
            </Button>
          </Link>
        </motion.section>

        {/* Behind the scenes */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold flex items-center gap-2 mb-6">
            <Cog className="size-6 text-primary" />
            What happens behind the scenes?
          </h2>
          <div className="space-y-6">
            <div className="rounded-xl border border-border/60 bg-card p-6">
              <h3 className="font-semibold text-foreground mb-2">
                1. Video upload
              </h3>
              <p className="text-muted-foreground text-sm">
                Large videos are uploaded efficiently (chunked & optimized).
              </p>
            </div>
            <div className="rounded-xl border border-border/60 bg-card p-6">
              <h3 className="font-semibold text-foreground mb-2">
                2. Processing pipeline
              </h3>
              <p className="text-muted-foreground text-sm mb-3">
                Once uploaded: multiple resolutions are generated (for adaptive
                streaming), HLS playlists are created, sprite sheets are
                generated, and metadata is stored.
              </p>
            </div>
            <div className="rounded-xl border border-border/60 bg-card p-6">
              <h3 className="font-semibold text-foreground mb-2">
                3. Smart playback
              </h3>
              <p className="text-muted-foreground text-sm">
                When a user presses play: HLS automatically selects the best
                quality, sprite sheet previews activate, and streaming adjusts
                in real time.
              </p>
            </div>
          </div>
        </motion.section>

        {/* Why I built this */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold mb-6">Why I built this</h2>
          <p className="text-muted-foreground leading-relaxed mb-4">
            Because watching tutorials wasn&apos;t enough. I wanted to
            understand how distributed video systems work, how performance
            optimizations actually improve UX, how real OTT systems scale, and
            how to design backend pipelines for heavy media. Streamflow is my
            hands-on exploration into real-world streaming engineering.
          </p>
        </motion.section>

        {/* Vision */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold mb-6">Vision for Streamflow</h2>
          <p className="text-muted-foreground mb-6">
            This project is evolving toward:
          </p>
          <ul className="grid gap-2 text-muted-foreground">
            {[
              "DRM-based content protection",
              "AI-powered recommendations",
              "Video analytics tracking",
              "Cloud-native microservices",
              "Production-level scalability",
            ].map((item) => (
              <li key={item} className="flex items-center gap-2">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" />
                {item}
              </li>
            ))}
          </ul>
          <p className="mt-6 text-muted-foreground italic">
            It&apos;s not just a side project. It&apos;s an engineering journey.
          </p>
        </motion.section>

        {/* Non-technical */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <h2 className="text-2xl font-semibold mb-6">
            For non-technical readers
          </h2>
          <p className="text-muted-foreground leading-relaxed">
            Imagine watching Netflix. Now imagine opening Netflix&apos;s engine
            room — seeing how videos change quality automatically, load
            instantly, show preview thumbnails, avoid buffering, and scale to
            millions of users. That&apos;s what Streamflow explores.
          </p>
        </motion.section>

        {/* Final thought - blockquote */}
        <motion.section {...fadeInUp} className="mt-16 md:mt-20">
          <blockquote className="relative border-l-4 border-primary pl-6 py-2 my-6">
            <Quote className="absolute -left-1 top-2 size-8 text-primary/30" />
            <p className="text-lg font-medium text-foreground italic">
              The best way to understand technology is to build it.
            </p>
          </blockquote>
          <p className="text-muted-foreground">
            One small Instagram reel led to: Curiosity → Research → Architecture
            → Implementation → Optimization. Streamflow exists because of that
            belief.
          </p>
        </motion.section>

        {/* CTA footer */}
        <motion.footer
          {...fadeInUp}
          className="mt-20 md:mt-24 pt-12 border-t border-border text-center"
        >
          <h2 className="text-2xl font-bold">Streamflow</h2>
          <p className="mt-2 text-muted-foreground">
            Built with curiosity. Engineered for depth. Designed to explore the
            future of streaming.
          </p>
        </motion.footer>
      </main>
    </div>
  );
}
