"use client";

import { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import api from "@/lib/axios";

/**
 * Order Tracking Page
 * Route: /track
 * Route: /track?order=ORD-20261001-0001 (pre-filled from confirmation page)
 *
 * Allows customers to track their order
 * by entering their order number.
 * No login required — public page.
 */
export default function TrackPage() {
  const searchParams = useSearchParams();

  // Initialize orderNumber from URL if available
  const [orderNumber, setOrderNumber] = useState(
    searchParams.get("order") || "",
  );
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // ── HANDLE TRACK — declared before useEffect ──
  const handleTrack = async (number) => {
    const trackNumber = number || orderNumber;

    if (!trackNumber.trim()) {
      setError("Please enter your order number");
      return;
    }

    setLoading(true);
    setError(null);
    setOrder(null);

    try {
      const response = await api.get(`/orders/track/${trackNumber.trim()}`);
      setOrder(response.data);
    } catch (err) {
      setError(
        err.response?.status === 404
          ? "Order not found. Please check your order number."
          : "Something went wrong. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  /**
   * If customer comes from order confirmed page
   * the order number is already in the URL as ?order=xxx
   * Auto-search on page load.
   */
  useEffect(() => {
    const orderFromUrl = searchParams.get("order");
    if (orderFromUrl) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      handleTrack(orderFromUrl);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /**
   * Returns the appropriate color and label
   * for each order status.
   */
  const getStatusInfo = (status) => {
    switch (status) {
      case "PROCESSING":
        return {
          color: "bg-yellow-100 text-yellow-700",
          label: "Processing",
          icon: "⏳",
        };
      case "SHIPPED":
        return {
          color: "bg-blue-100 text-blue-700",
          label: "Shipped",
          icon: "🚚",
        };
      case "DELIVERED":
        return {
          color: "bg-green-100 text-green-700",
          label: "Delivered",
          icon: "✅",
        };
      case "CANCELED":
        return {
          color: "bg-red-100 text-red-700",
          label: "Canceled",
          icon: "❌",
        };
      default:
        return {
          color: "bg-gray-100 text-gray-700",
          label: status,
          icon: "📦",
        };
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="text-center mb-10">
        <div className="text-5xl mb-4">📦</div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Track Your Order
        </h1>
        <p className="text-gray-500">
          Enter your order number to track your delivery.
        </p>
      </div>

      {/* ── SEARCH FORM ── */}
      <div className="bg-white rounded-2xl p-6 shadow-sm mb-8">
        <div className="flex gap-3">
          <input
            type="text"
            value={orderNumber}
            onChange={(e) => setOrderNumber(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleTrack()}
            placeholder="e.g. ORD-20261001-0001"
            className="flex-1 border border-gray-200 rounded-xl px-4 py-3 focus:outline-none focus:border-green-500 transition-colors"
          />
          <button
            onClick={() => handleTrack()}
            disabled={loading}
            className="bg-green-600 text-white px-6 py-3 rounded-xl font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-300"
          >
            {loading ? "..." : "Track"}
          </button>
        </div>

        {/* Error message */}
        {error && <p className="text-red-500 text-sm mt-3">{error}</p>}
      </div>

      {/* ── ORDER RESULT ── */}
      {order && (
        <div className="space-y-4">
          {/* Status Card */}
          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold text-gray-900">Order Status</h2>
              {(() => {
                const statusInfo = getStatusInfo(order.orderStatus);
                return (
                  <span
                    className={`${statusInfo.color} text-sm font-medium px-3 py-1 rounded-full flex items-center gap-1`}
                  >
                    {statusInfo.icon} {statusInfo.label}
                  </span>
                );
              })()}
            </div>

            <div className="space-y-3">
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">Order Number</span>
                <span className="font-bold text-green-600">
                  {order.orderNumber}
                </span>
              </div>
              {order.trackingNumber && (
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">Tracking Number</span>
                  <span className="font-bold text-green-600">
                    {order.trackingNumber}
                  </span>
                </div>
              )}
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">Payment</span>
                <span
                  className={`font-medium ${
                    order.paymentStatus === "PAID"
                      ? "text-green-600"
                      : "text-yellow-600"
                  }`}
                >
                  {order.paymentStatus === "PAID"
                    ? "Paid ✓"
                    : "Cash on Delivery"}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-gray-500">Total</span>
                <span className="font-bold text-gray-900">
                  {order.grandTotal} TND
                </span>
              </div>
            </div>
          </div>

          {/* Progress Timeline */}
          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <h2 className="text-lg font-bold text-gray-900 mb-6">
              Delivery Progress
            </h2>
            <div className="space-y-4">
              {[
                { status: "PROCESSING", label: "Order Placed", icon: "📋" },
                { status: "SHIPPED", label: "Shipped", icon: "🚚" },
                { status: "DELIVERED", label: "Delivered", icon: "✅" },
              ].map((step) => {
                const statuses = ["PROCESSING", "SHIPPED", "DELIVERED"];
                const currentIndex = statuses.indexOf(order.orderStatus);
                const stepIndex = statuses.indexOf(step.status);
                const isCompleted =
                  stepIndex <= currentIndex && order.orderStatus !== "CANCELED";
                const isCurrent =
                  stepIndex === currentIndex &&
                  order.orderStatus !== "CANCELED";

                return (
                  <div key={step.status} className="flex items-center gap-4">
                    <div
                      className={`w-10 h-10 rounded-full flex items-center justify-center text-lg flex-shrink-0 ${
                        isCompleted ? "bg-green-100" : "bg-gray-100"
                      } ${isCurrent ? "ring-2 ring-green-500" : ""}`}
                    >
                      {step.icon}
                    </div>
                    <div className="flex-1">
                      <p
                        className={`font-medium ${
                          isCompleted ? "text-gray-900" : "text-gray-400"
                        }`}
                      >
                        {step.label}
                      </p>
                    </div>
                    {isCompleted && (
                      <span className="text-green-500 text-sm font-medium">
                        ✓
                      </span>
                    )}
                  </div>
                );
              })}

              {/* Canceled state */}
              {order.orderStatus === "CANCELED" && (
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center text-lg">
                    ❌
                  </div>
                  <p className="font-medium text-red-600">Order Canceled</p>
                </div>
              )}
            </div>
          </div>

          {/* Shipping Info */}
          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <h2 className="text-lg font-bold text-gray-900 mb-4">
              Shipping To
            </h2>
            <div className="text-gray-600 space-y-1">
              <p className="font-semibold text-gray-900">
                {order.customerName}
              </p>
              <p>{order.shippingAddress}</p>
              <p>{order.city}</p>
              <p>{order.customerPhone}</p>
            </div>
          </div>

          {/* Items */}
          <div className="bg-white rounded-2xl p-6 shadow-sm">
            <h2 className="text-lg font-bold text-gray-900 mb-4">Items</h2>
            <div className="space-y-2">
              {order.items.map((item) => (
                <div key={item.id} className="flex justify-between text-sm">
                  <span className="text-gray-600">
                    {item.productTitle}{" "}
                    <span className="text-gray-400">× {item.quantity}</span>
                  </span>
                  <span className="font-medium">{item.subTotal} TND</span>
                </div>
              ))}
            </div>
          </div>

          {/* Back to home */}
          <Link
            href="/"
            className="block w-full border-2 border-green-600 text-green-600 py-4 rounded-full font-semibold text-center hover:bg-green-50 transition-colors"
          >
            ← Back to Home
          </Link>
        </div>
      )}
    </div>
  );
}
