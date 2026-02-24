const features = [
  {
    title: "Adaptive Streaming",
    description: "HLS/DASH with automatic quality adjustment",
  },
  {
    title: "DRM Protected",
    description: "Widevine & FairPlay encryption demo",
  },
  {
    title: "Preview Thumbnails",
    description: "Netflix-style seek preview",
  },
];

interface FeaturesProps {
  variant?: "light" | "dark";
}

export default function Features({ variant = "light" }: FeaturesProps) {
  const isDark = variant === "dark";
  return (
    <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6 text-left">
      {features.map((feature, i) => (
        <div
          key={i}
          className={`glass rounded-xl p-5 animate-slide-up border-2 text-center ${isDark ? "border-white/10" : "border-black/5"}`}
          style={{ animationDelay: `${i * 0.08}s` }}
        >
          <h3
            className={`text-base font-semibold mb-2 ${isDark ? "text-white" : "text-foreground"}`}
          >
            {feature.title}
          </h3>
          <p
            className={`text-sm leading-snug ${isDark ? "text-neutral-300" : "text-muted-foreground"}`}
          >
            {feature.description}
          </p>
        </div>
      ))}
    </div>
  );
}
