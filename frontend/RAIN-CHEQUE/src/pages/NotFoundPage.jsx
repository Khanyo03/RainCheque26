import React from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Home } from 'lucide-react';

const NotFoundPage = () => {
  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4 py-12">
      <div className="relative w-full max-w-lg border-8 border-black bg-white shadow-brutal-lg">
        <span aria-hidden="true" className="mx-sticker absolute -top-6 right-6 z-10 text-xs">
          Lost?
        </span>

        <div className="relative overflow-hidden border-b-8 border-black bg-black px-6 py-10 text-center">
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 bg-mx-stripes-red opacity-30"
          />
          <div className="relative">
            <div className="mx-auto mb-5 inline-flex h-20 w-20 items-center justify-center border-4 border-black bg-red-700 shadow-brutal-white">
              <AlertCircle className="w-10 h-10 text-white" />
            </div>
            <h1 className="font-display uppercase leading-none tracking-tighter text-7xl md:text-8xl text-white">
              404
            </h1>
          </div>
        </div>

        <div aria-hidden="true" className="h-3 bg-red-700" />

        <div className="p-8 text-center">
          <h2 className="font-display uppercase tracking-tighter text-2xl md:text-3xl text-black">
            Page Not Found
          </h2>
          <p className="mx-auto mt-4 max-w-sm text-xs font-bold uppercase tracking-widest text-gray-600">
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
          </p>

          <Link to="/" className="mx-btn-red mt-8">
            <Home className="w-5 h-5" aria-hidden="true" />
            Back to Home
          </Link>
        </div>
      </div>
    </div>
  );
};

export default NotFoundPage;
