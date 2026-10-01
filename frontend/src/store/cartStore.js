import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * Zustand Cart Store for BioTouch.
 *
 * What is Zustand?
 * A lightweight state management library.
 * Think of it as a global variable that:
 * → Any component can read from
 * → Any component can update
 * → Automatically re-renders components when it changes
 *
 * persist middleware → saves cart to localStorage
 * so it survives page refresh and browser close.
 *
 * Cart item structure:
 * {
 *   id: 1,
 *   title: "Shampooing Phytolisse - Normal",
 *   price: 79.99,
 *   imageUrl: "...",
 *   quantity: 2,
 *   slug: "shampooing-phytolisse-normal"
 * }
 */
const useCartStore = create(
  persist(
    (set, get) => ({
      /**
       * The cart items array.
       * Starts empty — loaded from localStorage on page load.
       */
      items: [],

      // ─────────────────────────────────────────────
      // ADD ITEM TO CART
      // ─────────────────────────────────────────────

      /**
       * Adds a product to the cart.
       * If product already exists → increase quantity.
       * If product is new → add it with quantity 1.
       *
       * @param product → the product object to add
       */
      addItem: (product) => {
        const items = get().items;
        const existingItem = items.find((item) => item.id === product.id);

        if (existingItem) {
          set({
            items: items.map((item) =>
              item.id === product.id
                ? { ...item, quantity: item.quantity + (product.quantity || 1) }
                : item,
            ),
          });
        } else {
          set({
            items: [...items, { ...product, quantity: product.quantity || 1 }],
          });
        }
      },

      // ─────────────────────────────────────────────
      // REMOVE ITEM FROM CART
      // ─────────────────────────────────────────────

      /**
       * Removes a product completely from the cart.
       * @param productId → the id of the product to remove
       */
      removeItem: (productId) => {
        set({
          items: get().items.filter((item) => item.id !== productId),
        });
      },

      // ─────────────────────────────────────────────
      // UPDATE QUANTITY
      // ─────────────────────────────────────────────

      /**
       * Updates the quantity of a specific cart item.
       * If quantity reaches 0 → remove from cart.
       *
       * @param productId → which product to update
       * @param quantity  → the new quantity
       */
      updateQuantity: (productId, quantity) => {
        if (quantity <= 0) {
          // Remove item if quantity is 0 or less
          get().removeItem(productId);
          return;
        }

        set({
          items: get().items.map((item) =>
            item.id === productId ? { ...item, quantity } : item,
          ),
        });
      },

      // ─────────────────────────────────────────────
      // CLEAR CART
      // ─────────────────────────────────────────────

      /**
       * Empties the entire cart.
       * Called after a successful order is placed.
       */
      clearCart: () => {
        set({ items: [] });
      },

      // ─────────────────────────────────────────────
      // COMPUTED VALUES
      // ─────────────────────────────────────────────

      /**
       * Total number of items in the cart.
       * Shown as a badge on the cart icon in the navbar.
       * e.g. if cart has 2 shampoos + 1 oil = 3
       */
      getTotalItems: () => {
        return get().items.reduce((total, item) => total + item.quantity, 0);
      },

      /**
       * Total price of all items in the cart.
       * Shown in the cart summary and checkout page.
       * e.g. 2 × 79.99 + 1 × 99.99 = 259.97
       */
      getTotalPrice: () => {
        return get().items.reduce(
          (total, item) => total + item.price * item.quantity,
          0,
        );
      },
    }),
    {
      /**
       * persist configuration:
       * name → the localStorage key where cart is saved
       * This means the cart is stored as:
       * localStorage["biotouch-cart"] = {...}
       */
      name: "biotouch-cart",
    },
  ),
);

export default useCartStore;
