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
    <ul className="mt-12 flex flex-col gap-4 text-left list-none">
      {features.map((feature, i) => (
        <li key={i} className="flex items-start gap-3">
          <span
            className="mt-2 size-2 shrink-0 rounded-full bg-red-500"
            aria-hidden
          />
          <div className="flex flex-col gap-0.5">
            <span className="text-base font-semibold text-white">
              {feature.title}
            </span>
            <span className="text-sm text-neutral-300">
              {feature.description}
            </span>
          </div>
        </li>
      ))}
    </ul>
  );
}
