import Link from "next/link";
import Image from "next/image";

export default function NotFound() {
  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center px-4">
      <div className="text-center max-w-md">
        {/* Logo */}
        <div className="flex justify-center mb-8">
          <Image
            src="/logo.png"
            alt="BioTouch"
            width={150}
            height={50}
            className="object-contain"
          />
        </div>

        {/* 404 */}
        <div className="text-8xl font-bold text-green-600 mb-4">404</div>

        {/* Message */}
        <h1 className="text-2xl font-bold text-gray-900 mb-3">
          Page Not Found
        </h1>
        <p className="text-gray-500 mb-8">
          The page you are looking for does not exist or has been moved.
        </p>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            href="/"
            className="bg-green-600 text-white px-8 py-3 rounded-full font-semibold hover:bg-green-700 transition-colors"
          >
            Back to Home
          </Link>
          <Link
            href="/track"
            className="border-2 border-green-600 text-green-600 px-8 py-3 rounded-full font-semibold hover:bg-green-50 transition-colors"
          >
            Track Order
          </Link>
        </div>
      </div>
    </div>
  );
}
