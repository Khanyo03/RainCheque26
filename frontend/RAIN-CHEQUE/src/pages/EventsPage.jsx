import React, { useState, useEffect } from 'react';
import { Search, Filter, Calendar, MapPin, X } from 'lucide-react';
import EventCard from '../components/EventCard';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';
import api from '../services/api';

const EventsPage = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState({
    category: '',
    city: '',
    date: '',
    priceRange: ''
  });
  const [showFilters, setShowFilters] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [categories, setCategories] = useState([]);
  const [cities, setCities] = useState([]);

  useEffect(() => {
    fetchEvents();
    fetchFilterOptions();
  }, [page, filters, searchTerm]);

  const fetchEvents = async () => {
    try {
      setLoading(true);
      const params = new URLSearchParams({
        page,
        size: 9,
        ...(searchTerm && { search: searchTerm }),
        ...(filters.category && { category: filters.category }),
        ...(filters.city && { city: filters.city }),
        ...(filters.date && { date: filters.date })
      });

      const response = await api.get(`/events?${params}`);
      setEvents(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
      setError(null);
    } catch (err) {
      setError('Failed to load events');
      console.error('Error fetching events:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchFilterOptions = async () => {
    try {
      const [categoriesRes, citiesRes] = await Promise.all([
        api.get('/events/categories'),
        api.get('/events/cities')
      ]);
      setCategories(categoriesRes.data || []);
      setCities(citiesRes.data || []);
    } catch (err) {
      console.error('Error fetching filter options:', err);
    }
  };

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    fetchEvents();
  };

  const handleFilterChange = (key, value) => {
    setFilters(prev => ({ ...prev, [key]: value }));
    setPage(0);
  };

  const clearFilters = () => {
    setFilters({
      category: '',
      city: '',
      date: '',
      priceRange: ''
    });
    setSearchTerm('');
    setPage(0);
  };

  return (
    <div className="max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <div className="border-4 border-black bg-black bg-mx-dots-red mx-dots-sm px-6 py-10 shadow-brutal-lg md:px-10 md:py-14">
          <h1 className="font-display uppercase tracking-tighter text-white text-4xl md:text-6xl">
            Discover Events
          </h1>
          <p className="mt-4 max-w-2xl font-bold uppercase tracking-wide text-sm text-gray-200 md:text-base">
            Find and book tickets for amazing events near you
          </p>
        </div>
        <div className="mt-4 h-3 w-full bg-red-700 border-4 border-black"></div>
      </div>

      {/* Search Bar */}
      <form onSubmit={handleSearch} className="mb-6">
        <div className="flex flex-col gap-4 md:flex-row">
          <div className="flex-1 relative">
            <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 text-black w-5 h-5 pointer-events-none" />
            <input
              type="text"
              placeholder="Search events by name, venue, or description..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="mx-input pl-12"
            />
          </div>
          <button
            type="button"
            onClick={() => setShowFilters(!showFilters)}
            className={`inline-flex items-center justify-center gap-2 border-4 border-black px-6 py-3 font-display uppercase tracking-tight shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none ${
              showFilters ? 'bg-red-700 text-white' : 'bg-white text-black'
            }`}
          >
            <Filter className="w-5 h-5" />
            <span>Filters</span>
          </button>
        </div>
      </form>

      {/* Filters Panel */}
      {showFilters && (
        <div className="border-4 border-black bg-white shadow-brutal p-6 mb-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-display uppercase tracking-tight text-lg">Filter Events</h3>
            <button
              onClick={clearFilters}
              className="border-4 border-black bg-white px-3 py-1 font-display uppercase tracking-widest text-xs text-black shadow-brutal-sm transition-transform duration-100 hover:-translate-y-[2px] active:translate-y-[2px] active:shadow-none"
            >
              Clear All
            </button>
          </div>
          <div className="grid md:grid-cols-4 gap-4">
            <div>
              <label className="mx-label">Category</label>
              <select
                value={filters.category}
                onChange={(e) => handleFilterChange('category', e.target.value)}
                className="mx-select"
              >
                <option value="">All Categories</option>
                {categories.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mx-label">City</label>
              <select
                value={filters.city}
                onChange={(e) => handleFilterChange('city', e.target.value)}
                className="mx-select"
              >
                <option value="">All Cities</option>
                {cities.map(city => (
                  <option key={city} value={city}>{city}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="mx-label">Date</label>
              <input
                type="date"
                value={filters.date}
                onChange={(e) => handleFilterChange('date', e.target.value)}
                className="mx-select"
              />
            </div>
            <div>
              <label className="mx-label">Price Range</label>
              <select
                value={filters.priceRange}
                onChange={(e) => handleFilterChange('priceRange', e.target.value)}
                className="mx-select"
              >
                <option value="">Any Price</option>
                <option value="0-25">Under $25</option>
                <option value="25-50">$25 - $50</option>
                <option value="50-100">$50 - $100</option>
                <option value="100+">$100+</option>
              </select>
            </div>
          </div>
        </div>
      )}

      {/* Active Filters */}
      {(searchTerm || Object.values(filters).some(v => v)) && (
        <div className="mb-6 flex flex-wrap items-center gap-2">
          <span className="font-display uppercase tracking-widest text-xs text-black">Active filters:</span>
          {searchTerm && (
            <span className="mx-tag bg-red-700 text-white">
              Search: {searchTerm}
              <button onClick={() => setSearchTerm('')} className="ml-2 hover:opacity-70">
                <X className="w-3 h-3" />
              </button>
            </span>
          )}
          {Object.entries(filters).map(([key, value]) => 
            value && (
              <span key={key} className="mx-tag bg-red-700 text-white">
                {key}: {value}
                <button onClick={() => handleFilterChange(key, '')} className="ml-2 hover:opacity-70">
                  <X className="w-3 h-3" />
                </button>
              </span>
            )
          )}
        </div>
      )}

      {/* Error Display */}
      {error && <ErrorAlert message={error} onClose={() => setError(null)} />}

      {/* Events Grid */}
      {loading ? (
        <LoadingSpinner />
      ) : events.length === 0 ? (
        <div className="text-center py-12">
          <div className="border-4 border-black bg-white shadow-brutal w-24 h-24 flex items-center justify-center mx-auto mb-6">
            <Calendar className="w-12 h-12 text-red-700" />
          </div>
          <h3 className="font-display uppercase tracking-tight text-xl text-black mb-2">No Events Found</h3>
          <p className="font-bold text-sm uppercase tracking-wide text-gray-700">Try adjusting your search or filters to find events.</p>
        </div>
      ) : (
        <>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {events.map(event => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-10 flex flex-wrap justify-center gap-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="border-4 border-black bg-white px-4 py-2 font-display uppercase tracking-tight text-sm text-black shadow-brutal-sm transition-transform duration-100 hover:-translate-y-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              >
                Previous
              </button>
              {[...Array(totalPages)].map((_, i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`border-4 border-black px-4 py-2 font-display uppercase tracking-tight text-sm shadow-brutal-sm transition-transform duration-100 hover:-translate-y-[2px] active:translate-y-[2px] active:shadow-none ${
                    page === i 
                      ? 'bg-red-700 text-white' 
                      : 'bg-white text-black'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page === totalPages - 1}
                className="border-4 border-black bg-white px-4 py-2 font-display uppercase tracking-tight text-sm text-black shadow-brutal-sm transition-transform duration-100 hover:-translate-y-[2px] active:translate-y-[2px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default EventsPage;