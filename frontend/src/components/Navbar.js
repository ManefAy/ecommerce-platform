"use client";

import Link from "next/link";
import Image from "next/image";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import useCartStore from "@/store/cartStore";

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [mounted, setMounted] = useState(false);
  const items = useCartStore((state) => state.items);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchOpen, setSearchOpen] = useState(false);
  const router = useRouter();

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  const totalItems = mounted
    ? items.reduce((total, item) => total + item.quantity, 0)
    : 0;

  const handleSearch = (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    router.push(`/search?keyword=${encodeURIComponent(searchQuery.trim())}`);
    setSearchQuery("");
    setSearchOpen(false);
  };

  return (
    <nav className="bg-white shadow-sm sticky top-0 z-50">
      <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
        {/* ── LOGO ── */}
        <Link href="/">
          <Image
            src="/logo.png"
            alt="BioTouch Cosmetics"
            width={150}
            height={48}
            className="object-contain"
            priority
          />
        </Link>

        {/* ── DESKTOP NAVIGATION ── */}
        <div className="hidden md:flex items-center gap-8">
          <Link
            href="/"
            className="text-gray-600 hover:text-green-700 transition-colors font-medium"
          >
            Home
          </Link>
          <Link
            href="/category/hair-care"
            className="text-gray-600 hover:text-green-700 transition-colors font-medium"
          >
            Hair Care
          </Link>
          <Link
            href="/category/skin-care"
            className="text-gray-600 hover:text-green-700 transition-colors font-medium"
          >
            Skin Care
          </Link>
          <Link
            href="/category/beauty"
            className="text-gray-600 hover:text-green-700 transition-colors font-medium"
          >
            Beauty
          </Link>
        </div>

        {/* ── RIGHT SIDE — CART + TRACK ── */}
        <div className="flex items-center gap-4">
          {/* Search Bar */}
          {searchOpen ? (
            <form onSubmit={handleSearch} className="flex items-center">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search products..."
                autoFocus
                className="border border-gray-200 rounded-full px-4 py-1.5 text-sm focus:outline-none focus:border-green-500 w-48"
              />
              <button
                type="button"
                onClick={() => setSearchOpen(false)}
                className="ml-2 text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </form>
          ) : (
            <button
              onClick={() => setSearchOpen(true)}
              className="hidden md:flex p-2 rounded-full hover:bg-green-50 transition-colors"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-5 w-5 text-gray-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </button>
          )}
          {/* Track Order */}
          <Link
            href="/track"
            className="hidden md:block text-gray-600 hover:text-green-700 transition-colors text-sm font-medium"
          >
            Track Order
          </Link>

          {/* Cart Icon with Badge */}
          <Link href="/cart" className="relative">
            <div className="p-2 rounded-full hover:bg-green-50 transition-colors">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6 text-gray-700"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
                />
              </svg>

              {/* Only render badge after client is mounted */}
              {mounted && totalItems > 0 && (
                <span className="absolute -top-1 -right-1 bg-green-600 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center font-bold">
                  {totalItems}
                </span>
              )}
            </div>
          </Link>

          {/* ── MOBILE HAMBURGER MENU ── */}
          <button
            className="md:hidden p-2"
            onClick={() => setMenuOpen(!menuOpen)}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6 text-gray-700"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              {menuOpen ? (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              ) : (
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M4 6h16M4 12h16M4 18h16"
                />
              )}
            </svg>
          </button>
        </div>
      </div>

      {/* ── MOBILE MENU ── */}
      {menuOpen && (
        <div className="md:hidden bg-white border-t border-gray-100 px-4 py-4 flex flex-col gap-4">
          <Link
            href="/"
            className="text-gray-600 hover:text-green-700 font-medium"
            onClick={() => setMenuOpen(false)}
          >
            Home
          </Link>
          <Link
            href="/category/hair-care"
            className="text-gray-600 hover:text-green-700 font-medium"
            onClick={() => setMenuOpen(false)}
          >
            Hair Care
          </Link>
          <Link
            href="/category/skin-care"
            className="text-gray-600 hover:text-green-700 font-medium"
            onClick={() => setMenuOpen(false)}
          >
            Skin Care
          </Link>
          <Link
            href="/category/beauty"
            className="text-gray-600 hover:text-green-700 font-medium"
            onClick={() => setMenuOpen(false)}
          >
            Beauty
          </Link>
          <Link
            href="/track"
            className="text-gray-600 hover:text-green-700 font-medium"
            onClick={() => setMenuOpen(false)}
          >
            Track Order
          </Link>
        </div>
      )}
    </nav>
  );
}
