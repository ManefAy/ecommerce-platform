/* eslint-disable react-hooks/exhaustive-deps */
"use client";

import api from "@/lib/axios";
import { useState, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { Toaster } from "react-hot-toast";

/**
 * Admin Dashboard Layout
 * Wraps all admin pages with:
 * → Sidebar navigation
 * → Top bar with admin name and logout
 * → Role verification (extra client-side check)
 *
 * This layout only renders for /admin/* routes
 * EXCEPT /admin/login which has its own layout.
 */
export default function AdminLayout({ children }) {
  const router = useRouter();
  const pathname = usePathname();
  const [admin, setAdmin] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  useEffect(() => {
    const adminUser = localStorage.getItem("admin_user");

    if (!adminUser) {
      router.push("/admin/login");
      return;
    }

    const user = JSON.parse(adminUser);

    if (user.role !== "ROLE_ADMIN") {
      router.push("/");
      return;
    }

    /**
     * Verify token is still valid by making a test API call.
     * If token expired → Spring Boot returns 401 or 403
     * → We clear everything and redirect to login.
     */
    api
      .get("/orders/admin/all", {
        headers: { Authorization: `Bearer ${user.token}` },
      })
      .then(() => {
        setAdmin(user);
      })
      .catch((err) => {
        if (err.response?.status === 401 || err.response?.status === 403) {
          /**
           * Token expired or invalid.
           * Clear everything and redirect to login.
           */
          document.cookie = "admin_token=; path=/; max-age=0; SameSite=Strict";
          localStorage.removeItem("admin_user");
          router.push("/admin/login");
        }
      });
  }, []);

  // Handle logout
  const handleLogout = () => {
    // Clear admin token cookie
    document.cookie = "admin_token=; path=/; max-age=0; SameSite=Strict";
    // Clear admin user from localStorage
    localStorage.removeItem("admin_user");
    // Redirect to admin login
    router.push("/admin/login");
  };

  // Don't render layout on login page
  if (pathname === "/admin/login") {
    return <>{children}</>;
  }

  // Loading state while checking auth
  if (!admin) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-gray-400">Loading...</div>
      </div>
    );
  }

  // Navigation items
  const navItems = [
    {
      href: "/admin",
      label: "Overview",
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
          />
        </svg>
      ),
    },
    {
      href: "/admin/orders",
      label: "Orders",
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
          />
        </svg>
      ),
    },
    {
      href: "/admin/products",
      label: "Products",
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"
          />
        </svg>
      ),
    },
    {
      href: "/admin/categories",
      label: "Categories",
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
          />
        </svg>
      ),
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50 flex">
      {/* ── SIDEBAR ── */}
      <aside
        className={`${
          sidebarOpen ? "w-64" : "w-16"
        } bg-white border-r border-gray-100 flex flex-col transition-all duration-300 fixed h-screen z-40 `}
      >
        {/* Logo */}
        <div className="p-4 border-b border-gray-100 flex items-center justify-between">
          {sidebarOpen && (
            <Image
              src="/logo.png"
              alt="BioTouch"
              width={120}
              height={40}
              className="object-contain"
            />
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 text-gray-500"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 6h16M4 12h16M4 18h16"
              />
            </svg>
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-3 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-xl transition-colors ${
                  isActive
                    ? "bg-green-50 text-green-700 font-medium"
                    : "text-gray-600 hover:bg-gray-50 hover:text-gray-900"
                }`}
              >
                {item.icon}
                {sidebarOpen && <span className="text-sm">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        {/* Admin info + Logout */}
        <div className="border-t border-gray-100 p-3 space-y-2">
          {/* Admin Info */}
          {sidebarOpen ? (
            <div className="bg-gray-50 rounded-xl px-3 py-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center flex-shrink-0">
                  <span className="text-green-700 text-sm font-bold">
                    {admin.fullName?.charAt(0).toUpperCase()}
                  </span>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-gray-900 truncate">
                    {admin.fullName}
                  </p>
                  <p className="text-xs text-gray-400 truncate">
                    {admin.email}
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex justify-center">
              <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                <span className="text-green-700 text-sm font-bold">
                  {admin.fullName?.charAt(0).toUpperCase()}
                </span>
              </div>
            </div>
          )}

          {/* Logout Button */}
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2 rounded-xl text-red-500 hover:bg-red-50 transition-colors"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-5 w-5 flex-shrink-0"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
              />
            </svg>
            {sidebarOpen && <span className="text-sm font-medium">Logout</span>}
          </button>
        </div>
      </aside>

      {/* ── MAIN CONTENT ── */}
      <main
        className={`flex-1 ${sidebarOpen ? "ml-64" : "ml-16"} transition-all duration-300`}
      >
        {/* Top Bar */}
        <header className="bg-white border-b border-gray-100 px-6 py-4 flex items-center justify-between sticky top-0 z-30">
          <h1 className="text-lg font-semibold text-gray-900">
            {navItems.find((item) => item.href === pathname)?.label || "Admin"}
          </h1>
          <div className="flex items-center gap-3">
            <span className="text-sm text-gray-500">
              Welcome, {admin.fullName}
            </span>
            <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
              <span className="text-green-700 text-sm font-bold">
                {admin.fullName?.charAt(0).toUpperCase()}
              </span>
            </div>
          </div>
        </header>

        {/* Page Content */}
        <div className="p-6">{children}</div>
      </main>
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3000,
          style: {
            borderRadius: "12px",
            background: "#fff",
            color: "#111",
            boxShadow: "0 4px 12px rgba(0,0,0,0.1)",
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
    </div>
  );
}
