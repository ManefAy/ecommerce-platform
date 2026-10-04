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

export default function RootLayout({ children }) {
  return (
    <html lang="fr">
      <body className={`${geist.className} bg-gray-50 min-h-screen`}>
        {children}
      </body>
    </html>
  );
}
