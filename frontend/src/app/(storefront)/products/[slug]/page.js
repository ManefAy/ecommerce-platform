"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import api from "@/lib/axios";
import useCartStore from "@/store/cartStore";

/**
 * Product Detail Page
 * Route: /products/[slug]
 * e.g. /products/shampooing-phytolisse-normal
 *
 * useParams() → reads the [slug] from the URL
 */
export default function ProductDetailPage() {
  const { slug } = useParams();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [quantity, setQuantity] = useState(1);
  const [added, setAdded] = useState(false);
  const addItem = useCartStore((state) => state.addItem);

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        const response = await api.get(`/products/slug/${slug}`);
        setProduct(response.data);
      } catch (error) {
        console.error("Failed to fetch product:", error);
      } finally {
        setLoading(false);
      }
    };

    if (slug) fetchProduct();
  }, [slug]);

  const handleAddToCart = () => {
    addItem({
      id: product.id,
      title: product.title,
      price: product.discountPrice || product.price,
      imageUrl: product.images?.[0]?.imageUrl || null,
      slug: product.slug,
      quantity: quantity,
    });

    // Show feedback
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  // ── LOADING STATE ──
  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-12 animate-pulse">
          <div className="bg-gray-200 rounded-2xl h-96" />
          <div className="space-y-4">
            <div className="bg-gray-200 h-8 rounded w-3/4" />
            <div className="bg-gray-200 h-4 rounded" />
            <div className="bg-gray-200 h-4 rounded w-2/3" />
          </div>
        </div>
      </div>
    );
  }

  // ── NOT FOUND ──
  if (!product) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16 text-center">
        <div className="text-6xl mb-4">🌿</div>
        <h1 className="text-2xl font-bold text-gray-900 mb-4">
          Product not found
        </h1>
        <Link
          href="/"
          className="text-green-600 hover:text-green-700 font-medium"
        >
          ← Back to Home
        </Link>
      </div>
    );
  }

  const currentPrice = product.discountPrice || product.price;

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">

      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-gray-500 mb-8">
        <Link href="/" className="hover:text-green-600">Home</Link>
        <span>/</span>
        <Link
          href={`/category/${product.categoryName?.toLowerCase().replace(" ", "-")}`}
          className="hover:text-green-600"
        >
          {product.categoryName}
        </Link>
        <span>/</span>
        <span className="text-gray-900">{product.title}</span>
      </nav>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-12">

        {/* ── PRODUCT IMAGE ── */}
        <div className="bg-gray-50 rounded-2xl p-8 flex items-center justify-center min-h-96">
          {product.images?.[0]?.imageUrl ? (
            <Image
              src={product.images[0].imageUrl}
              alt={product.title}
              width={400}
              height={400}
              className="object-contain max-h-80"
            />
          ) : (
            <div className="text-8xl">🌿</div>
          )}
        </div>

        {/* ── PRODUCT INFO ── */}
        <div className="flex flex-col justify-center">

          {/* Category badge */}
          <span className="text-sm text-green-600 font-medium uppercase tracking-wide mb-2">
            {product.categoryName}
          </span>

          {/* Title */}
          <h1 className="text-3xl font-bold text-gray-900 mb-4">
            {product.title}
          </h1>

          {/* Price */}
          <div className="flex items-center gap-3 mb-6">
            <span className="text-3xl font-bold text-green-600">
              {currentPrice} TND
            </span>
            {product.discountPrice && (
              <span className="text-xl text-gray-400 line-through">
                {product.price} TND
              </span>
            )}
            {product.discountPrice && (
              <span className="bg-red-100 text-red-600 text-sm font-medium px-2 py-1 rounded-full">
                Sale
              </span>
            )}
          </div>

          {/* Description */}
          {product.description && (
            <p className="text-gray-600 mb-8 leading-relaxed">
              {product.description}
            </p>
          )}

          {/* Stock status */}
          <div className="flex items-center gap-2 mb-6">
            {product.stockQuantity > 0 ? (
              <>
                <div className="w-2 h-2 bg-green-500 rounded-full" />
                <span className="text-sm text-green-600 font-medium">
                  In Stock ({product.stockQuantity} available)
                </span>
              </>
            ) : (
              <>
                <div className="w-2 h-2 bg-red-500 rounded-full" />
                <span className="text-sm text-red-600 font-medium">
                  Out of Stock
                </span>
              </>
            )}
          </div>

          {/* Quantity selector */}
          {product.stockQuantity > 0 && (
            <div className="flex items-center gap-4 mb-6">
              <span className="text-gray-700 font-medium">Quantity:</span>
              <div className="flex items-center border border-gray-200 rounded-full overflow-hidden">
                <button
                  onClick={() => setQuantity(Math.max(1, quantity - 1))}
                  className="px-4 py-2 hover:bg-gray-100 transition-colors font-bold"
                >
                  −
                </button>
                <span className="px-4 py-2 font-semibold min-w-12 text-center">
                  {quantity}
                </span>
                <button
                  onClick={() =>
                    setQuantity(Math.min(product.stockQuantity, quantity + 1))
                  }
                  className="px-4 py-2 hover:bg-gray-100 transition-colors font-bold"
                >
                  +
                </button>
              </div>
            </div>
          )}

          {/* Add to Cart Button */}
          <button
            onClick={handleAddToCart}
            disabled={product.stockQuantity === 0}
            className={`w-full py-4 rounded-full font-semibold text-lg transition-all ${
              added
                ? "bg-green-800 text-white"
                : "bg-green-600 text-white hover:bg-green-700"
            } disabled:bg-gray-300 disabled:cursor-not-allowed`}
          >
            {added ? "✓ Added to Cart!" : "Add to Cart"}
          </button>

          {/* Features */}
          <div className="grid grid-cols-3 gap-4 mt-8 pt-8 border-t border-gray-100">
            {[
              { icon: "🌿", label: "100% Natural" },
              { icon: "🚚", label: "Fast Delivery" },
              { icon: "💰", label: "Cash on Delivery" },
            ].map((feature, index) => (
              <div key={index} className="text-center">
                <div className="text-2xl mb-1">{feature.icon}</div>
                <div className="text-xs text-gray-500">{feature.label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}