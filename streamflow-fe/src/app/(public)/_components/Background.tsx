export default function Background() {
  return (
    <div className="absolute inset-0">
      <div
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{
          backgroundImage:
            "url(https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=1920&h=1080&fit=crop)",
        }}
      />
      <div className="absolute inset-0 bg-gradient-to-b from-white/92 via-white/75 to-white/90" />
      <div className="absolute inset-0 bg-gradient-to-r from-white/80 via-transparent to-white/80" />
    </div>
  );
}
