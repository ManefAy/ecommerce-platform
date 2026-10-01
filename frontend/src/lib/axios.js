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
 * Handles global errors:
 * → 401 Unauthorized → token expired → redirect to login
 * → 403 Forbidden → not enough permissions
 */
api.interceptors.response.use(
    (response) => {
    // Successful response → just return it
    return response;
    },
    (error) => {
    if (error.response?.status === 401) {
        /**
       * Token expired or invalid.
       * Clear the stored token and redirect to login.
       * This handles session expiry automatically.
       */
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        window.location.replace("/login");
    }

    return Promise.reject(error);
    },
);

export default api;
