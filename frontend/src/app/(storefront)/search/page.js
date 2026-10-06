"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import api from "@/lib/axios";
import useCartStore from "@/store/cartStore";
import toast from "react-hot-toast";

/**
 * Search Results Page
 * Route: /search?keyword=xxx
 *
 * Reads the keyword from URL query parameter
 * and fetches matching products from Spring Boot.
 */
export default function SearchPage() {
  const searchParams = useSearchParams();
  const keyword = searchParams.get("keyword") || "";
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(false);
  const addItem = useCartStore((state) => state.addItem);

  useEffect(() => {
    if (!keyword.trim()) return;

    const fetchResults = async () => {
      setLoading(true);
      try {
        const response = await api.get(
          `/products/search?keyword=${encodeURIComponent(keyword)}`,
        );
        setProducts(response.data);
      } catch (error) {
        console.error("Search failed:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchResults();
  }, [keyword]);

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

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Search Results</h1>
        {keyword && (
          <p className="text-gray-500 mt-2">
            {loading
              ? "Searching..."
              : `${products.length} result${products.length !== 1 ? "s" : ""} for "${keyword}"`}
          </p>
        )}
      </div>

      {/* Loading */}
      {loading && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-gray-200 h-64 rounded-2xl" />
          ))}
        </div>
      )}

      {/* No keyword */}
      {!keyword && (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🔍</div>
          <p className="text-xl text-gray-500">
            Enter a keyword to search products
          </p>
        </div>
      )}

      {/* No results */}
      {!loading && keyword && products.length === 0 && (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🌿</div>
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            No products found
          </h2>
          <p className="text-gray-500 mb-8">
            No results for &quot;{keyword}&quot;. Try a different keyword.
          </p>
          <Link
            href="/"
            className="bg-green-600 text-white px-8 py-3 rounded-full font-semibold hover:bg-green-700 transition-colors"
          >
            Back to Home
          </Link>
        </div>
      )}

      {/* Results Grid */}
      {!loading && products.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {products.map((product) => (
            <div
              key={product.id}
              className="bg-white rounded-2xl shadow-sm hover:shadow-md transition-shadow overflow-hidden group"
            >
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
