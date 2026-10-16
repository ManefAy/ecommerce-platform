import axios from "axios";

/**
 * Base Axios instance configured to talk to our Spring Boot API.
 *
 * baseURL → all requests automatically prepend this
 * so instead of writing:
 * axios.get("http://localhost:8080/api/products")
 * we just write:
 * api.get("/products")
 */
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
});

/**
 * REQUEST INTERCEPTOR
 * Runs before every request is sent.
 *
 * Automatically adds the JWT token to every request header.
 * This means we never have to manually add:
 * Authorization: Bearer token
 * to each API call — it happens automatically here.
 */
api.interceptors.request.use(
  (config) => {
    /**
     * Read the token from localStorage.
     * We store it there after login.
     * If no token → request goes through without it
     * (for public routes like product listing)
     */
    const token = localStorage.getItem("token");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

/**
 * RESPONSE INTERCEPTOR
 * Runs after every response is received.
 *
 * 401 Unauthorized:
 * → On a login/register request (/auth/...) it just means
 *   "wrong email or password" → let the page show the error,
 *   do NOT redirect.
 * → On an admin page it means the admin token expired →
 *   clear the admin session and go to /admin/login.
 * → Anywhere else (storefront) → clear the customer token.
 *   No redirect: there is no customer /login page yet.
 */
api.interceptors.response.use(
  (response) => {
    // Successful response → just return it
    return response;
  },
  (error) => {
    const isAuthRequest = error.config?.url?.startsWith("/auth/");

    if (error.response?.status === 401 && !isAuthRequest) {
      const onAdminPage = window.location.pathname.startsWith("/admin");

      if (onAdminPage) {
        // Admin token expired or invalid → back to admin login
        document.cookie = "admin_token=; path=/; max-age=0; SameSite=Strict";
        localStorage.removeItem("admin_user");
        if (window.location.pathname !== "/admin/login") {
          window.location.replace("/admin/login");
        }
      } else {
        // Storefront → just forget the stale customer token
        localStorage.removeItem("token");
        localStorage.removeItem("user");
      }
    }

    return Promise.reject(error);
  },
);

export default api;
