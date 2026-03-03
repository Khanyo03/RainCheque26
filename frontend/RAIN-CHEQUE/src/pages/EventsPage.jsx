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
        <h1 className="text-3xl font-bold text-gray-900 mb-4">Discover Events</h1>
        <p className="text-gray-600">Find and book tickets for amazing events near you</p>
      </div>

      {/* Search Bar */}
      <form onSubmit={handleSearch} className="mb-6">
        <div className="flex space-x-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              placeholder="Search events by name, venue, or description..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <button
            type="button"
            onClick={() => setShowFilters(!showFilters)}
            className={`px-6 py-3 border rounded-lg flex items-center space-x-2 ${
              showFilters ? 'bg-primary-600 text-white border-primary-600' : 'border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            <Filter className="w-5 h-5" />
            <span>Filters</span>
          </button>
        </div>
      </form>

      {/* Filters Panel */}
      {showFilters && (
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6 border">
          <div className="flex justify-between items-center mb-4">
            <h3 className="font-semibold text-lg">Filter Events</h3>
            <button
              onClick={clearFilters}
              className="text-sm text-primary-600 hover:text-primary-700"
            >
              Clear All
            </button>
          </div>
          <div className="grid md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Category</label>
              <select
                value={filters.category}
                onChange={(e) => handleFilterChange('category', e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">All Categories</option>
                {categories.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">City</label>
              <select
                value={filters.city}
                onChange={(e) => handleFilterChange('city', e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value="">All Cities</option>
                {cities.map(city => (
                  <option key={city} value={city}>{city}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Date</label>
              <input
                type="date"
                value={filters.date}
                onChange={(e) => handleFilterChange('date', e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Price Range</label>
              <select
                value={filters.priceRange}
                onChange={(e) => handleFilterChange('priceRange', e.target.value)}
                className="w-full border rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary-500"
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
        <div className="mb-6 flex flex-wrap gap-2">
          <span className="text-sm text-gray-600">Active filters:</span>
          {searchTerm && (
            <span className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full text-sm flex items-center">
              Search: {searchTerm}
              <button onClick={() => setSearchTerm('')} className="ml-2 hover:text-primary-900">
                <X className="w-3 h-3" />
              </button>
            </span>
          )}
          {Object.entries(filters).map(([key, value]) => 
            value && (
              <span key={key} className="bg-primary-100 text-primary-800 px-3 py-1 rounded-full text-sm flex items-center">
                {key}: {value}
                <button onClick={() => handleFilterChange(key, '')} className="ml-2 hover:text-primary-900">
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
          <div className="bg-gray-100 rounded-full w-24 h-24 flex items-center justify-center mx-auto mb-4">
            <Calendar className="w-12 h-12 text-gray-400" />
          </div>
          <h3 className="text-xl font-semibold text-gray-800 mb-2">No Events Found</h3>
          <p className="text-gray-600">Try adjusting your search or filters to find events.</p>
        </div>
      ) : (
        <>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {events.map(event => (
              <EventCard key={event.id} event={event} />
            ))}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="mt-8 flex justify-center space-x-2">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Previous
              </button>
              {[...Array(totalPages)].map((_, i) => (
                <button
                  key={i}
                  onClick={() => setPage(i)}
                  className={`px-4 py-2 border rounded-lg ${
                    page === i 
                      ? 'bg-primary-600 text-white border-primary-600' 
                      : 'hover:bg-gray-50'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page === totalPages - 1}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
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