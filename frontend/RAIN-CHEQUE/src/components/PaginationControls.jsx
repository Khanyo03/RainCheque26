import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

function PaginationControls({ currentPage, totalPages, onPageChange }) {
  const isFirstPage = currentPage <= 0;
  const isLastPage = currentPage >= totalPages - 1;

  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible) {
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Always show first page
      pages.push(0);

      let start = Math.max(1, currentPage - 1);
      let end = Math.min(totalPages - 2, currentPage + 1);

      // Adjust range to show enough pages
      if (currentPage <= 2) {
        end = 3;
      } else if (currentPage >= totalPages - 3) {
        start = totalPages - 4;
      }

      if (start > 1) {
        pages.push('ellipsis-start');
      }

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (end < totalPages - 2) {
        pages.push('ellipsis-end');
      }

      // Always show last page
      pages.push(totalPages - 1);
    }

    return pages;
  };

  if (totalPages <= 1) {
    return null;
  }

  return (
    <nav aria-label="Pagination" className="flex items-center justify-between gap-4">
      <p className="font-display uppercase tracking-widest text-xs text-black">
        Page {currentPage + 1} of {totalPages}
      </p>

      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={isFirstPage}
          aria-label="Previous page"
          className={`inline-flex items-center gap-1 border-4 border-black bg-white px-3 py-2 font-display uppercase text-xs tracking-widest shadow-brutal-sm transition-transform duration-100
            ${isFirstPage
              ? 'cursor-not-allowed opacity-50'
              : 'text-black hover:bg-gray-100 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none'
            }
          `}
        >
          <ChevronLeft className="w-4 h-4" />
          Previous
        </button>

        <div className="hidden sm:flex items-center gap-1">
          {getPageNumbers().map((page, index) => {
            if (typeof page === 'string') {
              return (
                <span key={page} className="px-2 py-1 font-display text-black">
                  &hellip;
                </span>
              );
            }

            return (
              <button
                key={page}
                onClick={() => onPageChange(page)}
                aria-label={`Page ${page + 1}`}
                aria-current={page === currentPage ? 'page' : undefined}
                className={`border-4 border-black px-3 py-1 font-display uppercase text-xs tracking-widest shadow-brutal-sm transition-transform duration-100 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none
                  ${page === currentPage
                    ? 'bg-red-700 text-white'
                    : 'bg-white text-black hover:bg-gray-100'
                  }
                `}
              >
                {page + 1}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={isLastPage}
          aria-label="Next page"
          className={`inline-flex items-center gap-1 border-4 border-black bg-white px-3 py-2 font-display uppercase text-xs tracking-widest shadow-brutal-sm transition-transform duration-100
            ${isLastPage
              ? 'cursor-not-allowed opacity-50'
              : 'text-black hover:bg-gray-100 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none'
            }
          `}
        >
          Next
          <ChevronRight className="w-4 h-4" />
        </button>
      </div>
    </nav>
  );
}

export default PaginationControls;
