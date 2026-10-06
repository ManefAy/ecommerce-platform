"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import useCartStore from "@/store/cartStore";
import api from "@/lib/axios";
import toast from "react-hot-toast";

/**
 * Checkout Page
 * Route: /checkout
 *
 * Collects customer shipping info and places the order.
 * Default payment: Cash on Delivery (COD)
 *
 * Flow:
 * 1. Customer fills shipping form
 * 2. Clicks "Place Order"
 * 3. We send POST /api/orders to Spring Boot
 * 4. Spring Boot saves order + calls Delivery API
 * 5. We redirect to /order-confirmed/[orderNumber]
 */
export default function CheckoutPage() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const items = useCartStore((state) => state.items);
  const clearCart = useCartStore((state) => state.clearCart);

  // Form state
  const [form, setForm] = useState({
    customerName: "",
    customerPhone: "",
    shippingAddress: "",
    city: "",
    notes: "",
  });

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  // Calculate total
  const totalPrice = mounted
    ? items.reduce((total, item) => total + item.price * item.quantity, 0)
    : 0;

  // Handle form field changes
  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // Handle form submission
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const loadingToast = toast.loading("Placing your order...");

    try {
      /**
       * Build the order request matching our
       * OrderRequest DTO in Spring Boot.
       */
      const orderRequest = {
        customerName: form.customerName,
        customerPhone: form.customerPhone,
        shippingAddress: form.shippingAddress,
        city: form.city,
        notes: form.notes,
        items: items.map((item) => ({
          productId: item.id,
          quantity: item.quantity,
        })),
      };

      // Send order to Spring Boot
      const response = await api.post("/orders", orderRequest);
      const order = response.data;

      toast.dismiss(loadingToast);
      toast.success("Order placed successfully!", { duration: 4000 });

      // Clear the cart after successful order
      clearCart();

      // Redirect to confirmation page
      router.push(`/order-confirmed/${order.orderNumber}`);
    } catch (err) {
      toast.dismiss(loadingToast);
      toast.error(
        err.response?.data?.message ||
          "Something went wrong. Please try again.",
      );
      setError(
        err.response?.data?.message ||
          "Something went wrong. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  // ── LOADING STATE ──
  if (!mounted) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16 animate-pulse">
        <div className="bg-gray-200 h-8 rounded w-48 mb-8" />
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="bg-gray-200 h-96 rounded-2xl" />
          <div className="bg-gray-200 h-96 rounded-2xl" />
        </div>
      </div>
    );
  }

  // ── EMPTY CART ──
  if (items.length === 0) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16 text-center">
        <div className="text-8xl mb-6">🛒</div>
        <h1 className="text-3xl font-bold text-gray-900 mb-4">
          Your cart is empty
        </h1>
        <Link
          href="/"
          className="bg-green-600 text-white px-8 py-3 rounded-full font-semibold hover:bg-green-700 transition-colors"
        >
          Continue Shopping
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      {/* Header */}
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Checkout</h1>

      <form onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* ── SHIPPING FORM ── */}
          <div className="space-y-6">
            <div className="bg-white rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold text-gray-900 mb-6">
                Shipping Information
              </h2>

              {/* Full Name */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Full Name *
                </label>
                <input
                  type="text"
                  name="customerName"
                  value={form.customerName}
                  onChange={handleChange}
                  required
                  placeholder="Your full name"
                  className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
                />
              </div>

              {/* Phone */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Phone Number *
                </label>
                <input
                  type="tel"
                  name="customerPhone"
                  value={form.customerPhone}
                  onChange={handleChange}
                  required
                  placeholder="e.g. 12345678"
                  className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
                />
              </div>

              {/* Address */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Shipping Address *
                </label>
                <input
                  type="text"
                  name="shippingAddress"
                  value={form.shippingAddress}
                  onChange={handleChange}
                  required
                  placeholder="Street address, apartment, etc."
                  className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
                />
              </div>

              {/* City */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  City *
                </label>
                <input
                  type="text"
                  name="city"
                  value={form.city}
                  onChange={handleChange}
                  required
                  placeholder="e.g. Tunis, Sfax, Sousse"
                  className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
                />
              </div>

              {/* Notes */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Order Notes (optional)
                </label>
                <textarea
                  name="notes"
                  value={form.notes}
                  onChange={handleChange}
                  placeholder="e.g. Please call before delivery"
                  rows={3}
                  className="w-full border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors resize-none"
                />
              </div>
            </div>

            {/* Payment Method */}
            <div className="bg-white rounded-2xl p-6 shadow-sm">
              <h2 className="text-xl font-bold text-gray-900 mb-4">
                Payment Method
              </h2>
              <div className="flex items-center gap-3 p-4 border-2 border-green-500 rounded-xl bg-green-50">
                <div className="w-5 h-5 bg-green-500 rounded-full flex items-center justify-center">
                  <div className="w-2 h-2 bg-white rounded-full" />
                </div>
                <div>
                  <p className="font-semibold text-gray-900">
                    Cash on Delivery
                  </p>
                  <p className="text-sm text-gray-500">
                    Pay when your order arrives
                  </p>
                </div>
                <span className="ml-auto text-2xl">💰</span>
              </div>
            </div>
          </div>

          {/* ── ORDER SUMMARY ── */}
          <div>
            <div className="bg-white rounded-2xl p-6 shadow-sm sticky top-24">
              <h2 className="text-xl font-bold text-gray-900 mb-6">
                Order Summary
              </h2>

              {/* Items list */}
              <div className="space-y-3 mb-6">
                {items.map((item) => (
                  <div key={item.id} className="flex justify-between text-sm">
                    <span className="text-gray-600">
                      {item.title}{" "}
                      <span className="text-gray-400">× {item.quantity}</span>
                    </span>
                    <span className="font-medium text-gray-900">
                      {(item.price * item.quantity).toFixed(2)} TND
                    </span>
                  </div>
                ))}
              </div>

              <div className="border-t border-gray-100 pt-4 space-y-3 mb-6">
                <div className="flex justify-between text-gray-600">
                  <span>Subtotal</span>
                  <span>{totalPrice.toFixed(2)} TND</span>
                </div>
                <div className="flex justify-between text-gray-600">
                  <span>Shipping</span>
                  <span className="text-green-600">Calculated by delivery</span>
                </div>
              </div>

              <div className="border-t border-gray-100 pt-4 mb-6">
                <div className="flex justify-between font-bold text-lg">
                  <span>Total</span>
                  <span className="text-green-600">
                    {totalPrice.toFixed(2)} TND
                  </span>
                </div>
              </div>

              {/* Error message */}
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-600 rounded-xl p-4 mb-4 text-sm">
                  {error}
                </div>
              )}

              {/* Place Order Button */}
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-green-600 text-white py-4 rounded-full font-semibold text-lg hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <svg
                      className="animate-spin h-5 w-5"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8v8z"
                      />
                    </svg>
                    Placing Order...
                  </span>
                ) : (
                  "Place Order — Cash on Delivery"
                )}
              </button>

              <p className="text-center text-xs text-gray-400 mt-4">
                By placing your order you agree to our terms and conditions.
              </p>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
