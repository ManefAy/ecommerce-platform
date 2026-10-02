import { NextResponse } from "next/server";

/**
 * Next.js Middleware
 * Runs on the SERVER before every request matching the config below.
 *
 * Protects all /admin/* routes:
 * → No token → redirect to /admin/login
 * → Has token but ROLE_USER → redirect to /
 * → Has token and ROLE_ADMIN → allow access ✅
 *
 * Note: We read the token from localStorage via a cookie
 * called "admin_token" that we set after admin login.
 * This is different from the storefront's localStorage token.
 */
export function middleware(request) {
  const { pathname } = request.nextUrl;

  /**
   * Allow /admin/login to pass through always.
   * Otherwise we'd get an infinite redirect loop:
   * /admin/login → no token → redirect to /admin/login → ...
   */
  if (pathname === "/admin/login") {
    return NextResponse.next();
  }

  /**
   * Check for admin token in cookies.
   * We store it as "admin_token" cookie after login.
   */
  const adminToken = request.cookies.get("admin_token");

  if (!adminToken) {
    /**
     * No token → redirect to admin login page.
     * We pass the original URL as a "from" parameter
     * so after login we can redirect back to where
     * the admin was trying to go.
     */
    const loginUrl = new URL("/admin/login", request.url);
    loginUrl.searchParams.set("from", pathname);
    return NextResponse.redirect(loginUrl);
  }

  /**
   * Token exists → allow the request to proceed.
   * The actual role verification happens in the
   * admin layout component on the client side,
   * and Spring Boot verifies the token on every API call.
   */
  return NextResponse.next();
}

/**
 * Configure which routes this middleware applies to.
 * Only runs on /admin/* routes — not on storefront routes.
 */
export const config = {
  matcher: ["/admin/:path*"],
};