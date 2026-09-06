import React from 'react';
import { AlertTriangle, X } from 'lucide-react';

const ErrorAlert = ({ message, onClose }) => {
  return (
    <div
      role="alert"
      className="mb-6 flex items-start gap-4 border-4 border-black bg-red-700 p-4 text-white shadow-brutal"
    >
      <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center border-4 border-black bg-white">
        <AlertTriangle className="h-5 w-5 text-red-700" aria-hidden="true" />
      </div>

      <div className="flex-1 pt-1">
        <p className="font-display uppercase tracking-widest text-xs text-black">Error</p>
        <p className="mt-1 text-sm font-bold">{message}</p>
      </div>

      {onClose && (
        <button
          onClick={onClose}
          aria-label="Dismiss error"
          className="flex h-8 w-8 flex-shrink-0 items-center justify-center border-4 border-black bg-black text-white transition-transform duration-100 hover:-translate-y-[2px] active:translate-y-[2px]"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      )}
    </div>
  );
};

export default ErrorAlert;
