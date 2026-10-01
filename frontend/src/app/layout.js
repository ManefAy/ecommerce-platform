import { Geist } from "next/font/google";
import "./globals.css";
import Navbar from "@/components/Navbar";

const geist = Geist({
  subsets: ["latin"],
});

export const metadata = {
  title: "BioTouch — Pure Nature, Beautiful You",
  description:
    "Découvrez nos produits naturels pour cheveux et beauté. 100% naturel, livraison rapide.",
};

/**
 * Root Layout — wraps every page in the app.
 *
 * Everything inside here appears on ALL pages:
 * → Navbar at the top
 * → Page content in the middle ({children})
 * → Footer at the bottom (we add it later)
 */
export default function RootLayout({ children }) {
  return (
    <html lang="fr">
      <body className={`${geist.className} bg-gray-50 min-h-screen`}>
        {/* Navbar appears on every page */}
        <Navbar />

        {/* Page specific content */}
        <main>{children}</main>
      </body>
    </html>
  );
}
