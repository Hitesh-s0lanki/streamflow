import Navbar from "./_components/navbar";

type ProtectedLayoutProps = {
  children: React.ReactNode;
};

export default function ProtectedLayout({ children }: ProtectedLayoutProps) {
  return (
    <div className="min-h-screen">
      <Navbar />
      {children}
    </div>
  );
}
