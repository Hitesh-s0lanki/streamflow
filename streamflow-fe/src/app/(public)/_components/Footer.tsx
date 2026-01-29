export default function Footer() {
  return (
    <footer className="relative z-10 py-4 px-4 text-center">
      <div className="flex items-center justify-center gap-4 text-sm text-muted-foreground">
        <a href="#" className="hover:text-foreground transition-colors">
          Privacy
        </a>
        <span>·</span>
        <a href="#" className="hover:text-foreground transition-colors">
          Terms
        </a>
        <span>·</span>
        <span>Demo — Not for production</span>
      </div>
    </footer>
  );
}
