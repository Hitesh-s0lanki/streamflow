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

export default function Features() {
  return (
    <div className="mt-12 grid grid-cols-1 md:grid-cols-3 gap-6 text-left">
      {features.map((feature, i) => (
        <div
          key={i}
          className="glass rounded-xl p-5 animate-slide-up border border-black/5"
          style={{ animationDelay: `${i * 0.08}s` }}
        >
          <h3 className="text-base font-semibold text-foreground mb-2">
            {feature.title}
          </h3>
          <p className="text-sm text-muted-foreground leading-snug">{feature.description}</p>
        </div>
      ))}
    </div>
  );
}
