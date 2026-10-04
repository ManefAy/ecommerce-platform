"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import api from "@/lib/axios";
import useCartStore from "@/store/cartStore";
import { useRouter } from "next/navigation";

/**
 * BioTouch Homepage
 *
 * Sections:
 * → Hero banner
 * → Featured products
 * → Categories
 */
export default function Home() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const addItem = useCartStore((state) => state.addItem);
  const router = useRouter();

  /**
   * Fetch all active products from Spring Boot API
   * when the page loads.
   */
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await api.get("/products");
        setProducts(response.data);
      } catch (error) {
        console.error("Failed to fetch products:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  /**
   * Add product to cart and show feedback.
   */
  const handleAddToCart = (product) => {
    addItem({
      id: product.id,
      title: product.title,
      price: product.discountPrice || product.price,
      imageUrl: product.images?.[0]?.imageUrl || null,
      slug: product.slug,
    });
    alert(`${product.title} added to cart!`);
  };

  return (
    <div className="min-h-screen">
      {/* ── HERO SECTION ── */}
      <section className="bg-gradient-to-r from-green-50 to-emerald-50 py-20">
        <div className="max-w-6xl mx-auto px-4 flex flex-col items-center text-center">
          <span className="bg-green-100 text-green-700 text-sm font-medium px-4 py-1 rounded-full mb-6">
            100% Natural Products
          </span>
          <h1 className="text-5xl font-bold text-gray-900 mb-6 leading-tight">
            Pure Nature, <span className="text-green-600">Beautiful You</span>
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl">
            Découvrez notre gamme de produits naturels pour cheveux et beauté.
            Formulés avec les meilleurs ingrédients de la nature.
          </p>
          {/* Search Bar */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              const keyword = e.target.keyword.value.trim();
              if (keyword)
                router.push(`/search?keyword=${encodeURIComponent(keyword)}`);
            }}
            className="flex items-center w-full max-w-md bg-white rounded-full shadow-sm overflow-hidden mb-6"
          >
            <input
              type="text"
              name="keyword"
              placeholder="Search natural products..."
              className="flex-1 px-6 py-3 focus:outline-none text-gray-700"
            />
            <button
              type="submit"
              className="bg-green-600 text-white px-6 py-3 hover:bg-green-700 transition-colors font-medium"
            >
              Search
            </button>
          </form>

          <div className="flex gap-4">
            <Link
              href="/category/hair-care"
              className="bg-green-600 text-white px-8 py-3 rounded-full font-semibold hover:bg-green-700 transition-colors"
            >
              Shop Now
            </Link>
            <Link
              href="/track"
              className="border-2 border-green-600 text-green-600 px-8 py-3 rounded-full font-semibold hover:bg-green-50 transition-colors"
            >
              Track Order
            </Link>
          </div>
        </div>
      </section>

      {/* ── FEATURES STRIP ── */}
      <section className="bg-white border-b border-gray-100">
        <div className="max-w-6xl mx-auto px-4 py-6 grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { icon: "🌿", text: "100% Natural" },
            { icon: "🚚", text: "Fast Delivery" },
            { icon: "💰", text: "Cash on Delivery" },
            { icon: "✨", text: "Premium Quality" },
          ].map((feature, index) => (
            <div
              key={index}
              className="flex items-center justify-center gap-2 text-gray-600"
            >
              <span className="text-2xl">{feature.icon}</span>
              <span className="font-medium text-sm">{feature.text}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ── FEATURED PRODUCTS ── */}
      <section className="max-w-6xl mx-auto px-4 py-16">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-3xl font-bold text-gray-900">
            Featured Products
          </h2>
          <Link
            href="/products"
            className="text-green-600 hover:text-green-700 font-medium"
          >
            View All →
          </Link>
        </div>

        {/* Loading state */}
        {loading && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-2xl p-4 animate-pulse">
                <div className="bg-gray-200 h-48 rounded-xl mb-4" />
                <div className="bg-gray-200 h-4 rounded mb-2" />
                <div className="bg-gray-200 h-4 rounded w-2/3" />
              </div>
            ))}
          </div>
        )}

        {/* Products grid */}
        {!loading && (
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
                  {/* Category */}
                  <span className="text-xs text-green-600 font-medium uppercase tracking-wide">
                    {product.categoryName}
                  </span>

                  {/* Title */}
                  <Link href={`/products/${product.slug}`}>
                    <h3 className="font-semibold text-gray-900 mt-1 mb-2 hover:text-green-600 transition-colors">
                      {product.title}
                    </h3>
                  </Link>

                  {/* Price */}
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

                  {/* Add to Cart Button */}
                  <button
                    onClick={() => handleAddToCart(product)}
                    disabled={product.stockQuantity === 0}
                    className="w-full bg-green-600 text-white py-2 rounded-full font-medium hover:bg-green-700 transition-colors disabled:bg-gray-300 disabled:cursor-not-allowed"
                  >
                    {product.stockQuantity === 0
                      ? "Out of Stock"
                      : "Add to Cart"}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty state */}
        {!loading && products.length === 0 && (
          <div className="text-center py-16 text-gray-500">
            <div className="text-6xl mb-4">🌿</div>
            <p className="text-xl">No products available yet.</p>
          </div>
        )}
      </section>
    </div>
  );
}
