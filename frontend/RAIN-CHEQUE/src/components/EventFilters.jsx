import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search } from 'lucide-react';

const STATUS_OPTIONS = ['All', 'DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED'];

function EventFilters({ onFilterChange, categories = [], initialFilters = {} }) {
  const [search, setSearch] = useState(initialFilters.search || '');
  const [category, setCategory] = useState(initialFilters.category || '');
  const [status, setStatus] = useState(initialFilters.status || '');
  const debounceTimer = useRef(null);
  const isFirstRender = useRef(true);

  const emitChange = useCallback((filters) => {
    if (onFilterChange) {
      onFilterChange(filters);
    }
  }, [onFilterChange]);

  // Debounced search
  useEffect(() => {
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }

    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }

    debounceTimer.current = setTimeout(() => {
      emitChange({ search, category, status });
    }, 300);

    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, [search, category, status, emitChange]);

  const handleCategoryChange = (e) => {
    const newCategory = e.target.value;
    setCategory(newCategory);
    // Immediate emit for dropdown changes
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }
    emitChange({ search, category: newCategory, status });
  };

  const handleStatusChange = (e) => {
    const newStatus = e.target.value;
    setStatus(newStatus);
    // Immediate emit for dropdown changes
    if (debounceTimer.current) {
      clearTimeout(debounceTimer.current);
    }
    emitChange({ search, category, status: newStatus });
  };

  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-4">
      {/* Search Input */}
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
        <input
          type="text"
          placeholder="Search events..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          aria-label="Search events"
          className="w-full pl-9 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* Category Dropdown */}
      <select
        value={category}
        onChange={handleCategoryChange}
        aria-label="Filter by category"
        className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
      >
        <option value="">All Categories</option>
        {categories.map((cat) => (
          <option key={cat} value={cat}>
            {cat}
          </option>
        ))}
      </select>

      {/* Status Dropdown */}
      <select
        value={status}
        onChange={handleStatusChange}
        aria-label="Filter by status"
        className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
      >
        <option value="">All Statuses</option>
        {STATUS_OPTIONS.filter(s => s !== 'All').map((s) => (
          <option key={s} value={s}>
            {s}
          </option>
        ))}
      </select>
    </div>
  );
}

export default EventFilters;
