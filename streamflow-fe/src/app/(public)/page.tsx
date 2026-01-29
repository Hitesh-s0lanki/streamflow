import Background from "./_components/Background";
import Header from "./_components/Header";
import Hero from "./_components/Hero";
import Footer from "./_components/Footer";

export default function Home() {
  return (
    <div className="ott-home relative min-h-screen flex flex-col overflow-hidden">
      <Background />
      <Header />
      <Hero />
      <Footer />
    </div>
  );
}
