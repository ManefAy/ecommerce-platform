"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Image from "next/image";
import api from "@/lib/axios";

/**
 * Admin Login Page
 * Route: /admin/login
 *
 * Separate from customer login — hidden from customers.
 * After successful login:
 * → Stores JWT in cookie (admin_token)
 * → Stores user info in localStorage
 * → Redirects to admin dashboard
 *
 * Only ROLE_ADMIN can access the dashboard.
 * ROLE_USER gets an error message.
 */
export default function AdminLoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await api.post("/auth/login", { email, password });
      const data = response.data;

      /**
       * Check if the user has ROLE_ADMIN.
       * ROLE_USER cannot access the admin dashboard.
       */
      if (data.role !== "ROLE_ADMIN") {
        setError("Access denied. Admin credentials required.");
        setLoading(false);
        return;
      }

      /**
       * Store the JWT token in a cookie named "admin_token".
       * The middleware reads this cookie to protect admin routes.
       *
       * We also store user info in localStorage for
       * displaying the admin name in the dashboard.
       *
       * Cookie settings:
       * → path=/ → available on all routes
       * → max-age=86400 → expires in 24 hours
       * → SameSite=Strict → CSRF protection
       */
      document.cookie = `admin_token=${data.token}; path=/; max-age=86400; SameSite=Strict`;

      localStorage.setItem(
        "admin_user",
        JSON.stringify({
          id: data.id,
          fullName: data.fullName,
          email: data.email,
          role: data.role,
          token: data.token,
        }),
      );

      /**
       * Redirect to where the admin was trying to go,
       * or to the dashboard if no redirect URL.
       */
      const from = searchParams.get("from") || "/admin";
      router.push(from);
    } catch (err) {
      setError(
        err.response?.status === 401
          ? "Invalid email or password."
          : "Something went wrong. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <Image
            src="/logo.png"
            alt="BioTouch"
            width={180}
            height={60}
            className="object-contain"
          />
        </div>

        {/* Login Card */}
        <div className="bg-white rounded-2xl shadow-sm p-8">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-gray-900">
              Admin Dashboard
            </h1>
            <p className="text-gray-500 mt-1 text-sm">
              Sign in with your admin credentials
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-5">
            {/* Email */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="admin@biotouch.com"
                className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
              />
            </div>

            {/* Password */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="••••••••"
                className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
              />
            </div>

            {/* Error message */}
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-600 rounded-xl p-4 text-sm">
                {error}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-green-600 text-white py-3 rounded-xl font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
            >
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>
        </div>

        {/* Back to store */}
        <p className="text-center text-sm text-gray-400 mt-6">
          <Link href="/" className="hover:text-green-600 transition-colors">
            ← Back to BioTouch Store
          </Link>
        </p>
      </div>
    </div>
  );
}
