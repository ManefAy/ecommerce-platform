"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import api from "@/lib/axios";

/**
 * Order Confirmed / Thank You Page
 * Route: /order-confirmed/[orderNumber]
 * e.g. /order-confirmed/ORD-20261001-0001
 *
 * Shows:
 * → Order confirmation message
 * → Order number and tracking number
 * → Order items summary
 * → What happens next
 */
export default function OrderConfirmedPage() {
  const { orderNumber } = useParams();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        const response = await api.get(`/orders/track/${orderNumber}`);
        setOrder(response.data);
      } catch (error) {
        console.error("Failed to fetch order:", error);
      } finally {
        setLoading(false);
      }
    };

    if (orderNumber) fetchOrder();
  }, [orderNumber]);

  // ── LOADING STATE ──
  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 animate-pulse">
        <div className="bg-gray-200 h-12 rounded-full w-12 mx-auto mb-6" />
        <div className="bg-gray-200 h-8 rounded w-3/4 mx-auto mb-4" />
        <div className="bg-gray-200 h-4 rounded w-1/2 mx-auto" />
      </div>
    );
  }

  // ── ORDER NOT FOUND ──
  if (!order) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-16 text-center">
        <div className="text-6xl mb-4">❌</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-4">
          Order not found
        </h1>
        <Link href="/" className="text-green-600 hover:text-green-700">
          ← Back to Home
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-12">
      {/* ── SUCCESS HEADER ── */}
      <div className="text-center mb-10">
        <div className="w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-6">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-10 w-10 text-green-600"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M5 13l4 4L19 7"
            />
          </svg>
        </div>
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Order Confirmed! 🎉
        </h1>
        <p className="text-gray-500">
          Thank you for your order. We will contact you shortly.
        </p>
      </div>

      {/* ── ORDER DETAILS ── */}
      <div className="bg-white rounded-2xl p-6 shadow-sm mb-6">
        <h2 className="text-lg font-bold text-gray-900 mb-4">Order Details</h2>

        <div className="space-y-3">
          <div className="flex justify-between">
            <span className="text-gray-500">Order Number</span>
            <span className="font-bold text-green-600">
              {order.orderNumber}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Status</span>
            <span className="bg-yellow-100 text-yellow-700 text-sm font-medium px-3 py-1 rounded-full">
              {order.orderStatus}
            </span>
          </div>
          <div className="flex justify-between">
            <span className="text-gray-500">Payment</span>
            <span className="text-gray-900 font-medium">
              {order.paymentMethod === "CASH_ON_DELIVERY"
                ? "Cash on Delivery"
                : "Card"}
            </span>
          </div>
          {order.trackingNumber && (
            <div className="flex justify-between">
              <span className="text-gray-500">Tracking Number</span>
              <span className="font-bold text-green-600">
                {order.trackingNumber}
              </span>
            </div>
          )}
          <div className="flex justify-between">
            <span className="text-gray-500">Total Amount</span>
            <span className="font-bold text-gray-900">
              {order.grandTotal} TND
            </span>
          </div>
        </div>
      </div>

      {/* ── SHIPPING INFO ── */}
      <div className="bg-white rounded-2xl p-6 shadow-sm mb-6">
        <h2 className="text-lg font-bold text-gray-900 mb-4">Shipping To</h2>
        <div className="space-y-2 text-gray-600">
          <p className="font-semibold text-gray-900">{order.customerName}</p>
          <p>{order.shippingAddress}</p>
          <p>{order.city}</p>
          <p>{order.customerPhone}</p>
          {order.notes && (
            <p className="text-sm text-gray-400 italic">Note: {order.notes}</p>
          )}
        </div>
      </div>

      {/* ── ORDER ITEMS ── */}
      <div className="bg-white rounded-2xl p-6 shadow-sm mb-6">
        <h2 className="text-lg font-bold text-gray-900 mb-4">Items Ordered</h2>
        <div className="space-y-3">
          {order.items.map((item) => (
            <div key={item.id} className="flex justify-between text-sm">
              <span className="text-gray-600">
                {item.productTitle}{" "}
                <span className="text-gray-400">× {item.quantity}</span>
              </span>
              <span className="font-medium text-gray-900">
                {item.subTotal} TND
              </span>
            </div>
          ))}
          <div className="border-t border-gray-100 pt-3 flex justify-between font-bold">
            <span>Total</span>
            <span className="text-green-600">{order.grandTotal} TND</span>
          </div>
        </div>
      </div>

      {/* ── WHAT HAPPENS NEXT ── */}
      <div className="bg-green-50 rounded-2xl p-6 mb-8">
        <h2 className="text-lg font-bold text-gray-900 mb-4">
          What happens next?
        </h2>
        <div className="space-y-3">
          {[
            {
              step: "1",
              text: "We will call you to confirm your order",
              icon: "📞",
            },
            {
              step: "2",
              text: "Your order will be prepared and shipped",
              icon: "📦",
            },
            {
              step: "3",
              text: "Delivery within 2-5 business days",
              icon: "🚚",
            },
            {
              step: "4",
              text: "Pay cash when your order arrives",
              icon: "💰",
            },
          ].map((item) => (
            <div key={item.step} className="flex items-center gap-3">
              <span className="text-2xl">{item.icon}</span>
              <span className="text-gray-700">{item.text}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── ACTIONS ── */}
      <div className="flex flex-col gap-3">
        <Link
          href={`/track?order=${order.orderNumber}`}
          className="block w-full bg-green-600 text-white py-4 rounded-full font-semibold text-center hover:bg-green-700 transition-colors"
        >
          Track My Order
        </Link>
        <Link
          href="/"
          className="block w-full border-2 border-green-600 text-green-600 py-4 rounded-full font-semibold text-center hover:bg-green-50 transition-colors"
        >
          Continue Shopping
        </Link>
      </div>
    </div>
  );
}
