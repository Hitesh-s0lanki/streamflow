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
    sm: "text-sm sm:text-base md:text-lg",
    md: "text-xl sm:text-2xl",
    lg: "text-2xl sm:text-3xl md:text-4xl",
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
        className="shrink-0 w-8 h-8 sm:w-9 sm:h-9 md:w-[70px] md:h-[70px]"
      />
      <span>StreamFlow</span>
    </Link>
  );
}
