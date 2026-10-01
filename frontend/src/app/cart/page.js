"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import useCartStore from "@/store/cartStore";

/**
 * Cart Page
 * Route: /cart
 *
 * Shows all items in the cart with:
 * → Product image, title, price
 * → Quantity controls
 * → Remove button
 * → Order summary with total
 * → Checkout button
 */
export default function CartPage() {
  const [mounted, setMounted] = useState(false);
  const items = useCartStore((state) => state.items);
  const removeItem = useCartStore((state) => state.removeItem);
  const updateQuantity = useCartStore((state) => state.updateQuantity);
  const clearCart = useCartStore((state) => state.clearCart);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  // Calculate totals
  const totalPrice = mounted
    ? items.reduce((total, item) => total + item.price * item.quantity, 0)
    : 0;

  const totalItems = mounted
    ? items.reduce((total, item) => total + item.quantity, 0)
    : 0;

  // ── LOADING STATE ──
  if (!mounted) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16">
        <div className="animate-pulse space-y-4">
          <div className="bg-gray-200 h-8 rounded w-48" />
          <div className="bg-gray-200 h-32 rounded" />
          <div className="bg-gray-200 h-32 rounded" />
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
        <p className="text-gray-500 mb-8">
          Discover our natural products and add them to your cart.
        </p>
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
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Your Cart
          <span className="text-gray-400 text-xl font-normal ml-2">
            ({totalItems} items)
          </span>
        </h1>
        <button
          onClick={clearCart}
          className="text-red-500 hover:text-red-600 text-sm font-medium transition-colors"
        >
          Clear Cart
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* ── CART ITEMS ── */}
        <div className="lg:col-span-2 space-y-4">
          {items.map((item) => (
            <div
              key={item.id}
              className="bg-white rounded-2xl p-4 flex items-center gap-4 shadow-sm"
            >
              {/* Product Image */}
              <div className="bg-gray-50 rounded-xl w-20 h-20 flex items-center justify-center flex-shrink-0">
                {item.imageUrl ? (
                  <Image
                    src={item.imageUrl}
                    alt={item.title}
                    width={80}
                    height={80}
                    className="object-contain"
                  />
                ) : (
                  <span className="text-3xl">🌿</span>
                )}
              </div>

              {/* Product Info */}
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-900 truncate">
                  {item.title}
                </h3>
                <p className="text-green-600 font-bold mt-1">
                  {item.price} TND
                </p>
              </div>

              {/* Quantity Controls */}
              <div className="flex items-center border border-gray-200 rounded-full overflow-hidden">
                <button
                  onClick={() => updateQuantity(item.id, item.quantity - 1)}
                  className="px-3 py-1 hover:bg-gray-100 transition-colors font-bold text-gray-600"
                >
                  −
                </button>
                <span className="px-3 py-1 font-semibold min-w-8 text-center">
                  {item.quantity}
                </span>
                <button
                  onClick={() => updateQuantity(item.id, item.quantity + 1)}
                  className="px-3 py-1 hover:bg-gray-100 transition-colors font-bold text-gray-600"
                >
                  +
                </button>
              </div>

              {/* Subtotal */}
              <div className="text-right flex-shrink-0">
                <p className="font-bold text-gray-900">
                  {(item.price * item.quantity).toFixed(2)} TND
                </p>
              </div>

              {/* Remove Button */}
              <button
                onClick={() => removeItem(item.id)}
                className="text-gray-400 hover:text-red-500 transition-colors flex-shrink-0"
              >
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
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            </div>
          ))}
        </div>

        {/* ── ORDER SUMMARY ── */}
        <div className="lg:col-span-1">
          <div className="bg-white rounded-2xl p-6 shadow-sm sticky top-24">
            <h2 className="text-xl font-bold text-gray-900 mb-6">
              Order Summary
            </h2>

            <div className="space-y-3 mb-6">
              <div className="flex justify-between text-gray-600">
                <span>Subtotal ({totalItems} items)</span>
                <span>{totalPrice.toFixed(2)} TND</span>
              </div>
              <div className="flex justify-between text-gray-600">
                <span>Shipping</span>
                <span className="text-green-600 font-medium">
                  Calculated at checkout
                </span>
              </div>
              <div className="flex justify-between text-gray-600">
                <span>Payment</span>
                <span className="text-green-600 font-medium">
                  Cash on Delivery
                </span>
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

            {/* Checkout Button */}
            <Link
              href="/checkout"
              className="block w-full bg-green-600 text-white py-4 rounded-full font-semibold text-center hover:bg-green-700 transition-colors"
            >
              Proceed to Checkout
            </Link>

            {/* Continue Shopping */}
            <Link
              href="/"
              className="block w-full text-center text-gray-500 hover:text-green-600 mt-4 text-sm transition-colors"
            >
              ← Continue Shopping
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
