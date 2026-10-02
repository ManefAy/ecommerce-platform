/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { useEffect, useState } from "react";
import api from "@/lib/axios";

/**
 * Admin Categories Management Page
 * Route: /admin/categories
 *
 * Features:
 * → View all categories with subcategories
 * → Add new category or subcategory
 * → Edit category
 * → Delete category
 */
export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const [form, setForm] = useState({
    name: "",
    slug: "",
    description: "",
    parentId: "",
  });

  // Get admin token
  const getAuthHeader = () => {
    const adminUser = JSON.parse(localStorage.getItem("admin_user") || "{}");
    return { Authorization: `Bearer ${adminUser.token}` };
  };

  const fetchCategories = async () => {
    try {
      const response = await api.get("/categories");
      setCategories(response.data);
    } catch (error) {
      console.error("Failed to fetch categories:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  /**
   * Auto-generate slug from name.
   * e.g. "Hair Care" → "hair-care"
   */
  const generateSlug = (name) => {
    return name
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, "")
      .replace(/\s+/g, "-")
      .replace(/-+/g, "-")
      .trim();
  };

  const handleNameChange = (e) => {
    const name = e.target.value;
    setForm({ ...form, name, slug: generateSlug(name) });
  };

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  // Open form for adding new category
  const handleAddNew = () => {
    setEditingCategory(null);
    setForm({ name: "", slug: "", description: "", parentId: "" });
    setError(null);
    setShowForm(true);
  };

  // Open form for editing existing category
  const handleEdit = (category, parentId = null) => {
    setEditingCategory(category);
    setForm({
      name: category.name,
      slug: category.slug,
      description: category.description || "",
      parentId: parentId || "",
    });
    setError(null);
    setShowForm(true);
  };

  // Save category (create or update)
  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      const payload = {
        name: form.name,
        slug: form.slug,
        description: form.description,
        parentId: form.parentId ? parseInt(form.parentId) : null,
      };

      if (editingCategory) {
        await api.put(`/categories/${editingCategory.id}`, payload, {
          headers: getAuthHeader(),
        });
      } else {
        await api.post("/categories", payload, {
          headers: getAuthHeader(),
        });
      }

      await fetchCategories();
      setShowForm(false);
    } catch (err) {
      setError(
        err.response?.data?.message ||
          "Failed to save category. Please try again.",
      );
    } finally {
      setSaving(false);
    }
  };

  // Delete category
  const handleDelete = async (categoryId) => {
    if (!confirm("Are you sure? This will also delete all subcategories."))
      return;

    try {
      await api.delete(`/categories/${categoryId}`, {
        headers: getAuthHeader(),
      });
      await fetchCategories();
    } catch (error) {
      console.error("Failed to delete category:", error);
      alert("Cannot delete category. It may have products assigned to it.");
    }
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
          {categories.length} categories total
        </p>
        <button
          onClick={handleAddNew}
          className="bg-green-600 text-white px-4 py-2 rounded-xl font-medium hover:bg-green-700 transition-colors flex items-center gap-2"
        >
          <span>+</span> Add Category
        </button>
      </div>

      {/* Categories List */}
      <div className="space-y-4">
        {categories.map((category) => (
          <div
            key={category.id}
            className="bg-white rounded-2xl shadow-sm overflow-hidden"
          >
            {/* Parent Category */}
            <div className="flex items-center justify-between p-4 border-b border-gray-50">
              <div>
                <h3 className="font-semibold text-gray-900">{category.name}</h3>
                <p className="text-xs text-gray-400">{category.slug}</p>
                {category.description && (
                  <p className="text-sm text-gray-500 mt-1">
                    {category.description}
                  </p>
                )}
              </div>
              <div className="flex items-center gap-3">
                <button
                  onClick={() => handleEdit(category)}
                  className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                >
                  Edit
                </button>
                <button
                  onClick={() => handleDelete(category.id)}
                  className="text-red-500 hover:text-red-600 text-sm font-medium"
                >
                  Delete
                </button>
                <button
                  onClick={() => {
                    setEditingCategory(null);
                    setForm({
                      name: "",
                      slug: "",
                      description: "",
                      parentId: category.id,
                    });
                    setError(null);
                    setShowForm(true);
                  }}
                  className="bg-green-50 text-green-600 hover:bg-green-100 text-sm font-medium px-3 py-1 rounded-lg transition-colors"
                >
                  + Add Subcategory
                </button>
              </div>
            </div>

            {/* Subcategories */}
            {category.subCategories?.length > 0 && (
              <div className="divide-y divide-gray-50">
                {category.subCategories.map((sub) => (
                  <div
                    key={sub.id}
                    className="flex items-center justify-between px-4 py-3 pl-8 bg-gray-50/50"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-gray-300 text-sm">└</span>
                        <span className="text-sm font-medium text-gray-700">
                          {sub.name}
                        </span>
                      </div>
                      <p className="text-xs text-gray-400 ml-4">{sub.slug}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => handleEdit(sub, category.id)}
                        className="text-blue-600 hover:text-blue-700 text-sm font-medium"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => handleDelete(sub.id)}
                        className="text-red-500 hover:text-red-600 text-sm font-medium"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Add/Edit Category Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-md">
            {/* Modal Header */}
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-lg font-bold text-gray-900">
                {editingCategory ? "Edit Category" : "Add New Category"}
              </h2>
              <button
                onClick={() => setShowForm(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSave} className="space-y-4">
              {/* Name */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Name *
                </label>
                <input
                  type="text"
                  value={form.name}
                  onChange={handleNameChange}
                  required
                  placeholder="e.g. Hair Care"
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
                  rows={2}
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500 resize-none"
                />
              </div>

              {/* Parent Category */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Parent Category (optional)
                </label>
                <select
                  name="parentId"
                  value={form.parentId}
                  onChange={handleChange}
                  className="w-full border border-gray-200 rounded-xl px-4 py-2.5 focus:outline-none focus:border-green-500"
                >
                  <option value="">None (top-level category)</option>
                  {categories.map((cat) => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
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
                    : editingCategory
                      ? "Save Changes"
                      : "Add Category"}
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
