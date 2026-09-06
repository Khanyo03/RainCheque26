import React, { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, Pencil, Trash2, Calendar } from 'lucide-react';
import { adminEventService } from '../services/api';
import EventFilters from '../components/EventFilters';
import PaginationControls from '../components/PaginationControls';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';

const STATUS_COLORS = {
  DRAFT: 'bg-white text-black',
  PUBLISHED: 'bg-red-700 text-white',
  CANCELLED: 'bg-black text-white',
  COMPLETED: 'bg-gray-300 text-black',
};

function StatusBadge({ status }) {
  const colorClass = STATUS_COLORS[status] || 'bg-white text-black';
  return (
    <span className={`inline-flex items-center border-2 border-black px-2.5 py-0.5 font-display text-[10px] uppercase tracking-widest ${colorClass}`}>
      {status}
    </span>
  );
}

function StatusDropdown({ currentStatus, onStatusChange }) {
  const statuses = ['DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED'];

  return (
    <select
      value={currentStatus}
      onChange={(e) => onStatusChange(e.target.value)}
      aria-label="Change event status"
      className="border-2 border-black bg-white px-2 py-1 font-display text-[10px] uppercase tracking-widest text-black focus:outline-none focus:shadow-brutal-sm"
    >
      {statuses.map((s) => (
        <option key={s} value={s}>
          {s}
        </option>
      ))}
    </select>
  );
}

function EventManagementPage() {
  const navigate = useNavigate();

  const [events, setEvents] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState({ search: '', category: '', status: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [categories, setCategories] = useState([]);
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  const fetchEvents = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        page: currentPage,
        size: 10,
        ...(filters.search && { search: filters.search }),
        ...(filters.category && { category: filters.category }),
        ...(filters.status && { status: filters.status }),
      };

      const response = await adminEventService.list(params);
      setEvents(response.data.content || []);
      setTotalPages(response.data.totalPages || 0);
    } catch (err) {
      setError('Failed to load events. Please try again.');
      console.error('Error fetching events:', err);
    } finally {
      setLoading(false);
    }
  }, [currentPage, filters]);

  const fetchCategories = useCallback(async () => {
    try {
      const response = await adminEventService.list({ size: 1000 });
      const allEvents = response.data.content || [];
      const uniqueCategories = [...new Set(allEvents.map((e) => e.category).filter(Boolean))];
      setCategories(uniqueCategories.sort());
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  }, []);

  useEffect(() => {
    fetchEvents();
  }, [fetchEvents]);

  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const handleFilterChange = useCallback((newFilters) => {
    setFilters(newFilters);
    setCurrentPage(0);
  }, []);

  const handlePageChange = useCallback((page) => {
    setCurrentPage(page);
  }, []);

  const handleDelete = async (eventId) => {
    try {
      await adminEventService.delete(eventId);
      setDeleteConfirm(null);
      fetchEvents();
    } catch (err) {
      setError('Failed to delete event. Please try again.');
      console.error('Error deleting event:', err);
    }
  };

  const handleStatusChange = async (eventId, newStatus) => {
    try {
      await adminEventService.updateStatus(eventId, newStatus);
      fetchEvents();
    } catch (err) {
      setError('Failed to update event status. Please try again.');
      console.error('Error updating status:', err);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-6 flex flex-col gap-4 border-4 border-black bg-black bg-mx-dots-red mx-dots-sm p-6 shadow-brutal-red sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-display text-3xl uppercase tracking-tighter text-white md:text-5xl">Event Management</h1>
        <Link
          to="/admin/events/new"
          className="inline-flex shrink-0 items-center gap-2 border-4 border-black bg-red-700 px-4 py-2 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
        >
          <Plus className="w-4 h-4" />
          Create New Event
        </Link>
      </div>

      {/* Filters */}
      <div className="mb-6">
        <EventFilters
          onFilterChange={handleFilterChange}
          categories={categories}
          initialFilters={filters}
        />
      </div>

      {/* Error */}
      {error && <ErrorAlert message={error} onClose={() => setError(null)} />}

      {/* Content */}
      {loading ? (
        <LoadingSpinner />
      ) : events.length === 0 ? (
        <div className="text-center py-12">
          <div className="mx-auto mb-4 flex h-24 w-24 items-center justify-center border-4 border-black bg-white shadow-brutal-sm">
            <Calendar className="w-12 h-12 text-red-700" />
          </div>
          <h3 className="mb-2 font-display text-xl uppercase tracking-tight text-black">No Events Found</h3>
          <p className="text-gray-600 mb-4">
            No events match your current filters, or you haven&apos;t created any events yet.
          </p>
          <Link
            to="/admin/events/new"
            className="inline-flex items-center gap-2 border-4 border-black bg-red-700 px-4 py-2 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
          >
            <Plus className="w-4 h-4" />
            Create Your First Event
          </Link>
        </div>
      ) : (
        <>
          {/* Desktop Table (lg and above) */}
          <div className="hidden lg:block overflow-x-auto border-4 border-black bg-white shadow-brutal">
            <table className="min-w-full">
              <thead className="bg-black">
                <tr>
                  <th scope="col" className="px-6 py-3 text-left font-display text-[11px] uppercase tracking-widest text-white">
                    Title
                  </th>
                  <th scope="col" className="px-6 py-3 text-left font-display text-[11px] uppercase tracking-widest text-white">
                    Category
                  </th>
                  <th scope="col" className="px-6 py-3 text-left font-display text-[11px] uppercase tracking-widest text-white">
                    Status
                  </th>
                  <th scope="col" className="px-6 py-3 text-left font-display text-[11px] uppercase tracking-widest text-white">
                    Date
                  </th>
                  <th scope="col" className="px-6 py-3 text-right font-display text-[11px] uppercase tracking-widest text-white">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y-4 divide-black">
                {events.map((event) => (
                  <tr key={event.id} className="hover:bg-gray-100">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="font-display text-sm uppercase tracking-tight text-black">{event.title}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-600">{event.category || '—'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <StatusBadge status={event.status} />
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-600">{formatDate(event.startTime)}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right">
                      <div className="flex items-center justify-end gap-2">
                        <StatusDropdown
                          currentStatus={event.status}
                          onStatusChange={(status) => handleStatusChange(event.id, status)}
                        />
                        <button
                          onClick={() => navigate(`/admin/events/${event.id}/edit`)}
                          aria-label={`Edit ${event.title}`}
                          className="border-2 border-black bg-white p-1.5 text-black transition-colors hover:bg-black hover:text-white"
                        >
                          <Pencil className="w-4 h-4" />
                        </button>
                        <button
                          onClick={() => setDeleteConfirm(event.id)}
                          aria-label={`Delete ${event.title}`}
                          className="border-2 border-black bg-white p-1.5 text-black transition-colors hover:bg-red-700 hover:text-white"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Mobile/Tablet Card Layout (below lg) */}
          <div className="lg:hidden space-y-4">
            {events.map((event) => (
              <div key={event.id} className="border-4 border-black bg-white shadow-brutal-sm p-4">
                <div className="flex items-start justify-between mb-2">
                  <h3 className="flex-1 pr-2 font-display text-sm uppercase tracking-tight text-black">{event.title}</h3>
                  <StatusBadge status={event.status} />
                </div>
                <div className="text-xs text-gray-500 space-y-1 mb-3">
                  <p>Category: {event.category || '—'}</p>
                  <p>Date: {formatDate(event.startTime)}</p>
                </div>
                <div className="flex items-center gap-2 pt-3 border-t-4 border-black">
                  <StatusDropdown
                    currentStatus={event.status}
                    onStatusChange={(status) => handleStatusChange(event.id, status)}
                  />
                  <button
                    onClick={() => navigate(`/admin/events/${event.id}/edit`)}
                    aria-label={`Edit ${event.title}`}
                    className="border-2 border-black bg-white p-1.5 text-black transition-colors hover:bg-black hover:text-white"
                  >
                    <Pencil className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => setDeleteConfirm(event.id)}
                    aria-label={`Delete ${event.title}`}
                    className="border-2 border-black bg-white p-1.5 text-black transition-colors hover:bg-red-700 hover:text-white"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Pagination */}
          <div className="mt-6">
            <PaginationControls
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        </>
      )}

      {/* Delete Confirmation Dialog */}
      {deleteConfirm !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="w-full max-w-sm mx-4 border-8 border-black bg-white shadow-brutal-lg p-6">
            <h3 className="mb-2 font-display text-lg uppercase tracking-tight text-black">Confirm Delete</h3>
            <p className="text-sm text-gray-600 mb-4">
              Are you sure you want to delete this event? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setDeleteConfirm(null)}
                className="border-4 border-black bg-white px-4 py-2 font-display text-sm uppercase tracking-tight text-black shadow-brutal-sm transition-transform duration-100 hover:bg-gray-100 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(deleteConfirm)}
                className="border-4 border-black bg-red-700 px-4 py-2 font-display text-sm uppercase tracking-tight text-white shadow-brutal-sm transition-transform duration-100 active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default EventManagementPage;
