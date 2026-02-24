"use client";

import Link from "next/link";
import { Github, Home, Info, Play } from "lucide-react";
import { Button } from "@/components/ui/button";

const NAV_LINKS = [
  { label: "Home", href: "/", icon: Home },
  { label: "GitHub", href: "https://github.com", icon: Github, external: true },
  { label: "About", href: "/about", icon: Info },
] as const;

const Navbar = () => {
  return (
    <header className="sticky top-0 z-50 w-full border-b bg-background/80 backdrop-blur-lg">
      <div className="mx-auto flex h-16 max-w-6xl items-center gap-4 px-4 sm:px-6">
        <div className="mr-auto flex items-center gap-3 overflow-hidden">
          <div className="bg-primary/10 flex size-9 shrink-0 items-center justify-center rounded-lg">
            <Play className="text-primary size-4" />
          </div>
          <div className="min-w-0">
            <h1 className="truncate text-base font-semibold leading-tight">
              StreamFlow
            </h1>
            <p className="text-muted-foreground truncate text-xs leading-tight">
              Upload &amp; manage your content
            </p>
          </div>
        </div>

        <nav className="hidden items-center gap-1 sm:flex">
          {NAV_LINKS.map(({ label, href, icon: Icon, ...rest }) => {
            const isExternal = "external" in rest;
            const Comp = isExternal ? "a" : Link;
            const externalProps = isExternal
              ? { target: "_blank", rel: "noopener noreferrer" }
              : {};

            return (
              <Button
                key={label}
                variant="ghost"
                size="default"
                asChild
                className="text-muted-foreground hover:text-foreground gap-1.5 text-sm font-medium"
              >
                <Comp href={href} {...externalProps}>
                  <Icon className="size-4" />
                  {label}
                </Comp>
              </Button>
            );
          })}
        </nav>

        <div className="flex items-center gap-1 sm:hidden">
          {NAV_LINKS.map(({ label, href, icon: Icon, ...rest }) => {
            const isExternal = "external" in rest;
            const Comp = isExternal ? "a" : Link;
            const externalProps = isExternal
              ? { target: "_blank", rel: "noopener noreferrer" }
              : {};

            return (
              <Button
                key={label}
                variant="ghost"
                size="icon-sm"
                asChild
                aria-label={label}
                className="text-muted-foreground hover:text-foreground"
              >
                <Comp href={href} {...externalProps}>
                  <Icon className="size-4" />
                </Comp>
              </Button>
            );
          })}
        </div>
      </div>
    </header>
  );
};

export default Navbar;
