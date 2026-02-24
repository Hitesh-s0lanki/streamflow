import Link from "next/link";
import { cn } from "@/lib/utils";
import Image from "next/image";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  variant?: "light" | "dark";
  className?: string;
}

export default function Logo({
  size = "md",
  variant = "light",
  className,
}: LogoProps) {
  const sizeClasses = {
    sm: "text-lg",
    md: "text-2xl",
    lg: "text-3xl md:text-4xl",
  };

  return (
    <Link
      href="/"
      className={cn(
        "font-bold flex items-center",
        sizeClasses[size],
        variant === "dark" && "text-white",
        className
      )}
    >
      <Image
        src="/logo.png"
        alt="StreamFlow"
        width={70}
        height={70}
        className="shrink-0"
      />
      <span>StreamFlow</span>
    </Link>
  );
}
