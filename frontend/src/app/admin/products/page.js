/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import api from "@/lib/axios";

/**
 * Admin Products Management Page
 * Route: /admin/products
 *
 * Features:
 * → View all products (including inactive)
 * → Add new product
 * → Edit product
 * → Toggle active/inactive
 * → Delete product
 */
export default function AdminProductsPage() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingProduct, setEditingProduct] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const [form, setForm] = useState({
    title: "",
    description: "",
    price: "",
    discountPrice: "",
    stockQuantity: "",
    weight: "",
    slug: "",
    active: true,
    categoryId: "",
  });

  // Get admin token
  const getAuthHeader = () => {
    const adminUser = JSON.parse(localStorage.getItem("admin_user") || "{}");
    return { Authorization: `Bearer ${adminUser.token}` };
  };

  const fetchData = async () => {
    try {
      const [productsRes, categoriesRes] = await Promise.all([
        api.get("/products/admin/all", { headers: getAuthHeader() }),
        api.get("/categories"),
      ]);
      setProducts(productsRes.data);
      setCategories(categoriesRes.data);
    } catch (error) {
      console.error("Failed to fetch data:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  /**
   * Auto-generate slug from title.
   * e.g. "Shampooing Phytolisse Normal" → "shampooing-phytolisse-normal"
   */
  const generateSlug = (title) => {
    return title
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, "")
      .replace(/\s+/g, "-")
      .replace(/-+/g, "-")
      .trim();
  };

  const handleTitleChange = (e) => {
    const title = e.target.value;
    setForm({
      ...form,
      title,
      slug: generateSlug(title),
    });
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({
      ...form,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  // Open form for adding new product
  const handleAddNew = () => {
    setEditingProduct(null);
    setForm({
      title: "",
      description: "",
      price: "",
      discountPrice: "",
      stockQuantity: "",
      weight: "",
      slug: "",
      active: true,
      categoryId: "",
    });
    setError(null);
    setShowForm(true);
  };

  // Open form for editing existing product
  const handleEdit = (product) => {
    setEditingProduct(product);
    setForm({
      title: product.title,
      description: product.description || "",
      price: product.price,
      discountPrice: product.discountPrice || "",
      stockQuantity: product.stockQuantity,
      weight: product.weight,
      slug: product.slug,
      active: product.active,
      categoryId: product.categoryId,
    });
    setError(null);
    setShowForm(true);
  };

  // Save product (create or update)
  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      const payload = {
        title: form.title,
        description: form.description,
        price: parseFloat(form.price),
        discountPrice: form.discountPrice
          ? parseFloat(form.discountPrice)
          : null,
        stockQuantity: parseInt(form.stockQuantity),
        weight: parseFloat(form.weight),
        slug: form.slug,
        active: form.active,
        categoryId: parseInt(form.categoryId),
      };

      if (editingProduct) {
        // Update existing product
        await api.put(`/products/${editingProduct.id}`, payload, {
          headers: getAuthHeader(),
        });
      } else {
        // Create new product
        await api.post("/products", payload, {
          headers: getAuthHeader(),
        });
      }

      await fetchData();
      setShowForm(false);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to save product. Please try again.",
      );
    } finally {
      setSaving(false);
    }
  };

  // Toggle product active status
  const handleToggle = async (productId) => {
    try {
      await api.patch(
        `/products/${productId}/toggle`,
        {},
        {
          headers: getAuthHeader(),
        },
      );
      await fetchData();
    } catch (error) {
      console.error("Failed to toggle product:", error);
    }
  };

  // Delete product
  const handleDelete = async (productId) => {
    if (!confirm("Are you sure you want to delete this product?")) return;
    try {
      await api.delete(`/products/${productId}`, {
        headers: getAuthHeader(),
      });
      await fetchData();
    } catch (error) {
      console.error("Failed to delete product:", error);
    }
  };

  // Flatten categories for select dropdown
  const getAllCategories = () => {
    const flat = [];
    categories.forEach((cat) => {
      flat.push(cat);
      if (cat.subCategories) {
        cat.subCategories.forEach((sub) => flat.push(sub));
      }
    });
    return flat;
  };

  if (loading) {
    return (
      <div className="space-y-4 animate-pulse">
        {[1, 2, 3].map((i) => (
          <div key={i} className="bg-gray-200 h-16 rounded-xl" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <p className="text-gray-500 text-sm">
          {products.length} products total
        </p>
        <button
          onClick={handleAddNew}
          className="bg-green-600 text-white px-4 py-2 rounded-xl font-medium hover:bg-green-700 transition-colors flex items-center gap-2"
        >
          <span>+</span> Add Product
        </button>
      </div>

      {/* Products Table */}
      <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-gray-100 bg-gray-50">
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Product
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Category
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Price
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Stock
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Status
                </th>
                <th className="text-left py-3 px-4 text-sm font-medium text-gray-500">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr
                  key={product.id}
                  className="border-b border-gray-50 hover:bg-gray-50 transition-colors"
                >
                  <td className="py-3 px-4">
                    <p className="text-sm font-medium text-gray-900">
                      {product.title}
                    </p>
                    <p className="text-xs text-gray-400">{product.slug}</p>
                  </td>
                  <td className="py-3 px-4 text-sm text-gray-600">
                    {product.categoryName}
                  </td>
                  <td className="py-3 px-4">
                    <p className="text-sm font-bold text-gray-900">
                      {product.discountPrice || product.price} TND
                    </p>
                    {product.discountPrice && (
                      <p className="text-xs text-gray-400 line-through">
                        {product.price} TND
                      </p>
                    )}
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`text-sm font-medium ${
                        product.stockQuantity === 0
                          ? "text-red-600"
                          : product.stockQuantity < 10
                            ? "text-orange-600"
                            : "text-gray-900"
                      }`}
                    >
                      {product.stockQuantity}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`text-xs font-medium px-2 py-1 rounded-full ${
                        product.active
                          ? "bg-green-100 text-green-700"
                          : "bg-gray-100 text-gray-500"
                      }`}
                    >
                      {product.active ? "Active" : "Inactive"}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleEdit(product)}
                        className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleToggle(product.id)}
                        className="text-orange-600 hover:text-orange-700 text-sm font-medium"
                      >
                        {product.active ? "Deactivate" : "Activate"}
                      </button>
                      <button
                        onClick={() => handleDelete(product.id)}
                        className="text-red-500 hover:text-red-600 text-sm font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add/Edit Product Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-2xl max-h-screen overflow-y-auto">
            {/* Modal Header */}
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-gray-900">
                {editingProduct ? "Edit Product" : "Add New Product"}
              </h2>
              <button
                onClick={() => setShowForm(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4">
              {/* Title */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Title *
                </label>
                <input
                  type="text"
                  name="title"
                  value={form.title}
                  onChange={handleTitleChange}
                  required
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                />
              </div>

              {/* Slug */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Slug *
                </label>
                <input
                  type="text"
                  name="slug"
                  value={form.slug}
                  onChange={handleChange}
                  required
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500 bg-gray-50"
                />
              </div>

              {/* Description */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description
                </label>
                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  rows={3}
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500 resize-none"
                />
              </div>

              {/* Price + Discount Price */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Price (TND) *
                  </label>
                  <input
                    type="number"
                    name="price"
                    value={form.price}
                    onChange={handleChange}
                    required
                    step="0.01"
                    min="0"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Discount Price (TND)
                  </label>
                  <input
                    type="number"
                    name="discountPrice"
                    value={form.discountPrice}
                    onChange={handleChange}
                    step="0.01"
                    min="0"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                  />
                </div>
              </div>

              {/* Stock + Weight */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Stock Quantity *
                  </label>
                  <input
                    type="number"
                    name="stockQuantity"
                    value={form.stockQuantity}
                    onChange={handleChange}
                    required
                    min="0"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Weight (KG) *
                  </label>
                  <input
                    type="number"
                    name="weight"
                    value={form.weight}
                    onChange={handleChange}
                    required
                    step="0.01"
                    min="0"
                    className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                  />
                </div>
              </div>

              {/* Category */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Category *
                </label>
                <select
                  name="categoryId"
                  value={form.categoryId}
                  onChange={handleChange}
                  required
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                >
                  <option value="">Select a category</option>
                  {getAllCategories().map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.parentId ? `  └ ${cat.name}` : cat.name}
                    </option>
                  ))}
                </select>
              </div>

              {/* Active */}
              <div className="flex items-center gap-3">
                <input
                  type="checkbox"
                  name="active"
                  id="active"
                  checked={form.active}
                  onChange={handleChange}
                  className="w-4 h-4 accent-green-600"
                />
                <label
                  htmlFor="active"
                  className="text-sm font-medium text-gray-700"
                >
                  Active (visible in storefront)
                </label>
              </div>

              {/* Error */}
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-600 rounded-xl p-3 text-sm">
                  {error}
                </div>
              )}

              {/* Buttons */}
              <div className="flex gap-3 pt-2">
                <button
                  type="submit"
                  disabled={saving}
                  className="flex-1 bg-green-600 text-white py-2.5 rounded-xl font-medium hover:bg-green-700 transition-colors disabled:bg-gray-300"
                >
                  {saving
                    ? "Saving..."
                    : editingProduct
                      ? "Save Changes"
                      : "Add Product"}
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="flex-1 bg-gray-100 text-gray-700 py-2.5 rounded-xl font-medium hover:bg-gray-200 transition-colors"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
