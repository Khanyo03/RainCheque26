import React from 'react';

const LoadingSpinner = ({ fullPage = false }) => {
  const spinner = (
    <div className="flex flex-col items-center justify-center gap-4 p-8">
      <div
        className="h-16 w-16 animate-spin border-8 border-black border-t-red-700"
        role="status"
        aria-label="Loading"
      ></div>
      <span className="animate-blink font-display uppercase tracking-widest text-sm text-black">
        Loading
      </span>
    </div>
  );

  if (fullPage) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-white/80">
        <div className="border-8 border-black bg-white px-10 py-8 shadow-brutal-lg">
          {spinner}
        </div>
      </div>
    );
  }

  return spinner;
};

export default LoadingSpinner;
