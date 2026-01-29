import Link from "next/link";
import { cn } from "@/lib/utils";
import Image from "next/image";

interface LogoProps {
  size?: "sm" | "md" | "lg";
  className?: string;
}

export default function Logo({ size = "md", className }: LogoProps) {
  const sizeClasses = {
    sm: "text-lg",
    md: "text-2xl",
    lg: "text-3xl md:text-4xl",
  };

  return (
    <Link
      href="/"
      className={cn(
        "font-bold flex items-center gap-2",
        sizeClasses[size],
        className,
      )}
    >
      <Image src="/logo.png" alt="StreamFlow" width={70} height={70} />
      StreamFlow
    </Link>
  );
}
