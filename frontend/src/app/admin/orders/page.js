/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import api from "@/lib/axios";
import toast from "react-hot-toast";

/**
 * Admin Orders Management Page
 * Route: /admin/orders
 *
 * Features:
 * → View all orders
 * → Filter by status
 * → Update order status
 * → Update payment status
 * → Cancel order
 */
export default function AdminOrdersPage() {
  const [orders, setOrders] = useState([]);
  const [filteredOrders, setFilteredOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [updating, setUpdating] = useState(false);

  // Get admin token
  const getAuthHeader = () => {
    const adminUser = JSON.parse(localStorage.getItem("admin_user") || "{}");
    return { Authorization: `Bearer ${adminUser.token}` };
  };

  const fetchOrders = async () => {
    try {
      const response = await api.get("/orders/admin/all", {
        headers: getAuthHeader(),
      });
      setOrders(response.data);
      setFilteredOrders(response.data);
    } catch (error) {
      console.error("Failed to fetch orders:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Filter orders by status
  useEffect(() => {
    if (statusFilter === "ALL") {
      setFilteredOrders(orders);
    } else {
      setFilteredOrders(orders.filter((o) => o.orderStatus === statusFilter));
    }
  }, [statusFilter, orders]);

  const fetchAndUpdateSelected = async (orderId) => {
    // 1. Fetch all fresh orders from Spring Boot
    const response = await api.get("/orders/admin/all", {
      headers: getAuthHeader(),
    });
    const updatedOrders = response.data;

    // 2. Update the orders table
    setOrders(updatedOrders);
    setFilteredOrders(updatedOrders);

    // 3. Find the current order in fresh data
    const updated = updatedOrders.find((o) => o.id === orderId);

    // 4. Update the modal with fresh data
    if (updated) setSelectedOrder(updated);
  };

  // Update order status
  const handleUpdateStatus = async (orderId, newStatus) => {
    setUpdating(true);
    try {
      await api.patch(
        `/orders/admin/${orderId}/status?newStatus=${newStatus}`,
        {},
        { headers: getAuthHeader() },
      );
      await fetchAndUpdateSelected(orderId);
    } catch (error) {
      console.error("Failed to update status:", error);
    } finally {
      setUpdating(false);
    }
  };

  // Update payment status
  const handleUpdatePayment = async (orderId, newStatus) => {
    setUpdating(true);
    try {
      await api.patch(
        `/orders/admin/${orderId}/payment?newStatus=${newStatus}`,
        {},
        { headers: getAuthHeader() },
      );
      await fetchAndUpdateSelected(orderId);
    } catch (error) {
      console.error("Failed to update payment:", error);
    } finally {
      setUpdating(false);
    }
  };

  // Cancel order
  const handleCancelOrder = async (orderId) => {
    if (!confirm("Are you sure you want to cancel this order?")) return;
    setUpdating(true);
    try {
      await api.patch(
        `/orders/admin/${orderId}/cancel`,
        {},
        { headers: getAuthHeader() },
      );
      await fetchAndUpdateSelected(orderId);
    } catch (error) {
      console.error("Failed to cancel order:", error);
      toast.error(
        "Cannot cancel this order. Only PROCESSING orders can be canceled.",
      );
    } finally {
      setUpdating(false);
    }
  };

  const getStatusBadge = (status) => {
    const styles = {
      PROCESSING: "bg-yellow-100 text-yellow-700",
      SHIPPED: "bg-blue-100 text-blue-700",
      DELIVERED: "bg-green-100 text-green-700",
      CANCELED: "bg-red-100 text-red-700",
    };
    return styles[status] || "bg-gray-100 text-gray-700";
  };

  const getPaymentBadge = (status) => {
    return status === "PAID"
      ? "bg-green-100 text-green-700"
      : "bg-orange-100 text-orange-700";
  };

  if (loading) {
    return (
      <div className="space-y-4 animate-pulse">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="bg-gray-200 h-16 rounded-xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header + Filter */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <p className="text-gray-500 text-sm">
          {filteredOrders.length} orders found
        </p>

        {/* Status Filter */}
        <div className="flex flex-wrap gap-2">
          {["ALL", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELED"].map(
            (status) => (
              <button
                key={status}
                onClick={() => setStatusFilter(status)}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${
                  statusFilter === status
                    ? "bg-green-600 text-white"
                    : "bg-white text-gray-600 hover:bg-gray-50 border border-gray-200"
                }`}
              >
                {status}
              </button>
            ),
          )}
        </div>
      </div>

      {/* Orders Table */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Order
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Customer
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  City
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Total
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Payment
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Status
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Date
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {filteredOrders.map((order) => (
                <tr
                  key={order.id}
                  className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                >
                  <td className="py-3 px-4 text-sm font-medium text-green-600">
                    {order.orderNumber}
                  </td>
                  <td className="py-3 px-4">
                    <p className="text-sm font-medium text-gray-900">
                      {order.customerName}
                    </p>
                    <p className="text-xs text-gray-400">
                      {order.customerPhone}
                    </p>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-600">
                    {order.city}
                  </td>
                  <td className="py-3 px-4 text-sm font-bold text-gray-900">
                    {order.grandTotal} TND
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`text-xs font-medium px-2 py-1 rounded-full ${getPaymentBadge(order.paymentStatus)}`}
                    >
                      {order.paymentStatus}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`text-xs font-medium px-2 py-1 rounded-full ${getStatusBadge(order.orderStatus)}`}
                    >
                      {order.orderStatus}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-500">
                    {new Date(order.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3 px-4">
                    <button
                      onClick={() => setSelectedOrder(order)}
                      className="text-green-600 hover:text-green-700 text-sm font-medium"
                    >
                      Manage
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Order Detail Modal */}
      {selectedOrder && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-lg max-h-screen overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-gray-900">
                {selectedOrder.orderNumber}
              </h2>
              <button
                onClick={() => setSelectedOrder(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            {/* Customer Info */}
            <div className="bg-gray-50 rounded-xl p-4 mb-4">
              <p className="font-semibold text-gray-900">
                {selectedOrder.customerName}
              </p>
              <p className="text-sm text-gray-500">
                {selectedOrder.customerPhone}
              </p>
              <p className="text-sm text-gray-500">
                {selectedOrder.shippingAddress}
              </p>
              <p className="text-sm text-gray-500">{selectedOrder.city}</p>
              {selectedOrder.notes && (
                <p className="text-sm text-gray-400 italic mt-1">
                  Note: {selectedOrder.notes}
                </p>
              )}
            </div>

            {/* Order Items */}
            <div className="mb-4">
              <h3 className="text-sm font-medium text-gray-700 mb-2">Items</h3>
              {selectedOrder.items.map((item) => (
                <div
                  key={item.id}
                  className="flex justify-between text-sm py-1"
                >
                  <span className="text-gray-600">
                    {item.productTitle} × {item.quantity}
                  </span>
                  <span className="font-medium">{item.subTotal} TND</span>
                </div>
              ))}
              <div className="border-t border-gray-100 pt-2 mt-2 flex justify-between font-bold">
                <span>Total</span>
                <span className="text-green-600">
                  {selectedOrder.grandTotal} TND
                </span>
              </div>
            </div>

            {/* Update Order Status */}
            {selectedOrder.orderStatus !== "CANCELED" && (
              <div className="mb-4">
                <h3 className="text-sm font-medium text-gray-700 mb-2">
                  Update Order Status
                </h3>
                <div className="flex flex-wrap gap-2">
                  {["PROCESSING", "SHIPPED", "DELIVERED"].map((status) => (
                    <button
                      key={status}
                      onClick={() =>
                        handleUpdateStatus(selectedOrder.id, status)
                      }
                      disabled={
                        updating || selectedOrder.orderStatus === status
                      }
                      className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                        selectedOrder.orderStatus === status
                          ? "bg-green-600 text-white"
                          : "bg-gray-100 text-gray-600 hover:bg-green-50 hover:text-green-700"
                      } disabled:opacity-50`}
                    >
                      {status}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Update Payment Status */}
            <div className="mb-4">
              <h3 className="text-sm font-medium text-gray-700 mb-2">
                Update Payment Status
              </h3>
              <div className="flex gap-2">
                {["UNPAID", "PAID"].map((status) => (
                  <button
                    key={status}
                    onClick={() =>
                      handleUpdatePayment(selectedOrder.id, status)
                    }
                    disabled={
                      updating || selectedOrder.paymentStatus === status
                    }
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                      selectedOrder.paymentStatus === status
                        ? "bg-green-600 text-white"
                        : "bg-gray-100 text-gray-600 hover:bg-green-50 hover:text-green-700"
                    } disabled:opacity-50`}
                  >
                    {status}
                  </button>
                ))}
              </div>
            </div>

            {/* Cancel Order */}
            {selectedOrder.orderStatus === "PROCESSING" && (
              <button
                onClick={() => handleCancelOrder(selectedOrder.id)}
                disabled={updating}
                className="w-full bg-red-50 text-red-600 py-2.5 rounded-xl font-medium hover:bg-red-100 transition-colors disabled:opacity-50"
              >
                Cancel Order
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
