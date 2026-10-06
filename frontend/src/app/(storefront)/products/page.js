"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import api from "@/lib/axios";
import useCartStore from "@/store/cartStore";
import toast from "react-hot-toast";

/**
 * All Products Page
 * Route: /products
 *
 * Shows all active products in a grid.
 */
export default function ProductsPage() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const addItem = useCartStore((state) => state.addItem);

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

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 animate-pulse">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div key={i} className="bg-gray-200 h-64 rounded-2xl" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-4 py-12">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">All Products</h1>
        <p className="text-gray-500 mt-2">
          {products.length} products available
        </p>
      </div>

      {/* Products Grid */}
      {products.length === 0 ? (
        <div className="text-center py-16">
          <div className="text-6xl mb-4">🌿</div>
          <p className="text-xl text-gray-500">No products available yet.</p>
        </div>
      ) : (
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
