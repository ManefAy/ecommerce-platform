"use client";

import { useEffect, useState } from "react";
import api from "@/lib/axios";

/**
 * Admin Dashboard Overview Page
 * Route: /admin
 *
 * Shows key metrics:
 * → Total orders
 * → Orders by status
 * → Recent orders
 */
export default function AdminDashboardPage() {
  const [stats, setStats] = useState({
    totalOrders: 0,
    processing: 0,
    shipped: 0,
    delivered: 0,
    canceled: 0,
  });
  const [recentOrders, setRecentOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Get admin user token
        const adminUser = JSON.parse(
          localStorage.getItem("admin_user") || "{}",
        );

        // Set token for this request
        const response = await api.get("/orders/admin/all", {
          headers: {
            Authorization: `Bearer ${adminUser.token}`,
          },
        });

        const orders = response.data;

        // Calculate stats
        setStats({
          totalOrders: orders.length,
          processing: orders.filter((o) => o.orderStatus === "PROCESSING")
            .length,
          shipped: orders.filter((o) => o.orderStatus === "SHIPPED").length,
          delivered: orders.filter((o) => o.orderStatus === "DELIVERED").length,
          canceled: orders.filter((o) => o.orderStatus === "CANCELED").length,
        });

        // Get 5 most recent orders
        setRecentOrders(orders.slice(0, 5));
      } catch (error) {
        console.error("Failed to fetch dashboard data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const statCards = [
    {
      label: "Total Orders",
      value: stats.totalOrders,
      color: "bg-blue-50 text-blue-700",
      icon: "📦",
    },
    {
      label: "Processing",
      value: stats.processing,
      color: "bg-yellow-50 text-yellow-700",
      icon: "⏳",
    },
    {
      label: "Shipped",
      value: stats.shipped,
      color: "bg-purple-50 text-purple-700",
      icon: "🚚",
    },
    {
      label: "Delivered",
      value: stats.delivered,
      color: "bg-green-50 text-green-700",
      icon: "✅",
    },
  ];

  const getStatusBadge = (status) => {
    const styles = {
      PROCESSING: "bg-yellow-100 text-yellow-700",
      SHIPPED: "bg-blue-100 text-blue-700",
      DELIVERED: "bg-green-100 text-green-700",
      CANCELED: "bg-red-100 text-red-700",
    };
    return styles[status] || "bg-gray-100 text-gray-700";
  };

  if (loading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-gray-200 h-28 rounded-2xl" />
          ))}
        </div>
        <div className="bg-gray-200 h-64 rounded-2xl" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => (
          <div key={card.label} className="bg-white rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <span className="text-2xl">{card.icon}</span>
              <span
                className={`text-xs font-medium px-2 py-1 rounded-full ${card.color}`}
              >
                {card.label}
              </span>
            </div>
            <p className="text-3xl font-bold text-gray-900">{card.value}</p>
          </div>
        ))}
      </div>

      {/* Recent Orders */}
      <div className="bg-white rounded-2xl shadow-sm p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-gray-900">Recent Orders</h2>
          <a
            href="/admin/orders"
            className="text-green-600 hover:text-green-700 text-sm font-medium"
          >
            View All →
          </a>
        </div>

        {recentOrders.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <p>No orders yet</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-100">
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Order
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Customer
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Total
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Status
                  </th>
                  <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                    Date
                  </th>
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((order) => (
                  <tr
                    key={order.id}
                    className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                  >
                    <td className="py-3 px-4 text-sm font-medium text-green-600">
                      {order.orderNumber}
                    </td>
                    <td className="py-3 px-4 text-sm text-gray-700">
                      {order.customerName}
                    </td>
                    <td className="py-3 px-4 text-sm font-medium text-gray-900">
                      {order.grandTotal} TND
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
