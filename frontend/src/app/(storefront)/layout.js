import Navbar from "@/components/Navbar";
import Footer from "@/components/Footer";
import {Toaster} from "react-hot-toast";

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
      <Toaster
        position="top-right"
        toastOptions={{
          durattion: 3000,
          style: {
            borderRadius: "12px",
            background: "#fff",
            color: "#111",
            boxShadow: "0  4 12px rgba(0, 0, 0, 0.1)",
          },
          success: {
            iconTheme: {
              primary: "#16a34a",
              secondary: "#fff",
            },
          },
          error: {
            iconTheme: {
              primary: "#ef4444",
              secondary: "#fff",
            },
          },
        }}
        />
    </>
  );
}
