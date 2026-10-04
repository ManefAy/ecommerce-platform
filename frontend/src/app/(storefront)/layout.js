import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";

/**
 * Storefront Layout
 * Wraps only customer-facing pages with Navbar and Footer.
 * Admin pages use their own layout without Navbar or Footer.
 */
export default function StorefrontLayout({ children }) {
  return (
    <>
      <Navbar />
      <main>{children}</main>
      <Footer />
    </>
  );
}
