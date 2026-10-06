"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import api from "@/lib/axios";
import useCartStore from "@/store/cartStore";
import toast from "react-hot-toast";

/**
 * Category Page
 * Route: /category/[slug]
 * e.g. /category/hair-care
 *
 * Shows all products in a category
 * including products in subcategories.
 */
export default function CategoryPage() {
  const { slug } = useParams();
  const [category, setCategory] = useState(null);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const addItem = useCartStore((state) => state.addItem);
  const [notFoundError, setNotFoundError] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch category details
        const categoryRes = await api.get(`/categories/slug/${slug}`);
        setCategory(categoryRes.data);

        // Fetch products in this category
        const productsRes = await api.get(
          `/products/category/${categoryRes.data.id}`,
        );
        setProducts(productsRes.data);
      } catch (error) {
        console.error("Failed to fetch category:", error);
        setNotFoundError(true);
      } finally {
        setLoading(false);
      }
    };

    if (slug) fetchData();
  }, [slug]);

  const handleAddToCart = (product) => {
    addItem({
      id: product.id,
      title: product.title,
      price: product.discountPrice || product.price,
      imageUrl: product.images?.[0]?.imageUrl || null,
      slug: product.slug,
    });
    toast.success(`${product.title} added to cart!`);
  };

  // ── LOADING STATE ──
  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16">
        <div className="animate-pulse">
          <div className="bg-gray-200 h-8 rounded w-48 mb-8" />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-gray-200 h-64 rounded-2xl" />
            ))}
          </div>
        </div>
      </div>
    );
  }

  // 3. Not found check — outside useEffect
  if (notFoundError) {
    return (
      <div className="min-h-screen flex items-center justify-center px-4">
        <div className="text-center max-w-md">
          <div className="text-8xl font-bold text-green-600 mb-4">404</div>
          <h1 className="text-2xl font-bold text-gray-900 mb-3">
            Category Not Found
          </h1>
          <p className="text-gray-500 mb-8">
            This category does not exist or has been removed.
          </p>
          <Link
            href="/"
            className="bg-green-600 text-white px-8 py-3 rounded-full font-semibold hover:bg-green-700 transition-colors"
          >
            Back to Home
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-8">
        <Link href="/" className="hover:text-green-600">
          Home
        </Link>
        <span>/</span>
        <span className="text-gray-900">{category?.name}</span>
      </nav>

      {/* Category Header */}
      <div className="mb-10">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          {category?.name}
        </h1>
        {category?.description && (
          <p className="text-gray-500 max-w-2xl">{category.description}</p>
        )}

        {/* Subcategories */}
        {category?.subCategories?.length > 0 && (
          <div className="flex flex-wrap gap-2 mt-4">
            {category.subCategories.map((sub) => (
              <Link
                key={sub.id}
                href={`/category/${sub.slug}`}
                className="bg-green-50 text-green-700 px-4 py-2 rounded-full text-sm font-medium hover:bg-green-100 transition-colors"
              >
                {sub.name}
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Products count */}
      <p className="text-gray-500 mb-6">
        {products.length} product{products.length !== 1 ? "s" : ""} found
      </p>

      {/* ── PRODUCTS GRID ── */}
      {products.length === 0 ? (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🌿</div>
          <p className="text-xl text-gray-500">
            No products in this category yet.
          </p>
          <Link
            href="/"
            className="text-green-600 hover:text-green-700 font-medium mt-4 inline-block"
          >
            ← Back to Home
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product) => (
            <div
              key={product.id}
              className="bg-white rounded-2xl shadow-sm hover:shadow-md transition-shadow overflow-hidden group"
            >
              {/* Product Image */}
              <Link href={`/products/${product.slug}`}>
                <div className="bg-gray-50 h-48 flex items-center justify-center overflow-hidden">
                  {product.images?.[0]?.imageUrl ? (
                    <Image
                      src={product.images[0].imageUrl}
                      alt={product.title}
                      width={200}
                      height={200}
                      className="object-contain h-full w-full group-hover:scale-105 transition-transform"
                    />
                  ) : (
                    <div className="text-6xl">🌿</div>
                  )}
                </div>
              </Link>

              {/* Product Info */}
              <div className="p-4">
                <span className="text-xs text-green-600 font-medium uppercase tracking-wide">
                  {product.categoryName}
                </span>

                <Link href={`/products/${product.slug}`}>
                  <h3 className="font-semibold text-gray-900 mt-1 mb-2 hover:text-green-600 transition-colors">
                    {product.title}
                  </h3>
                </Link>

                <div className="flex items-center gap-2 mb-4">
                  {product.discountPrice ? (
                    <>
                      <span className="text-xl font-bold text-green-600">
                        {product.discountPrice} TND
                      </span>
                      <span className="text-sm text-gray-400 line-through">
                        {product.price} TND
                      </span>
                    </>
                  ) : (
                    <span className="text-xl font-bold text-green-600">
                      {product.price} TND
                    </span>
                  )}
                </div>

                <button
                  onClick={() => handleAddToCart(product)}
                  disabled={product.stockQuantity === 0}
                  className="w-full bg-green-600 text-white py-2 rounded-full font-medium hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
                >
                  {product.stockQuantity === 0 ? "Out of Stock" : "Add to Cart"}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
