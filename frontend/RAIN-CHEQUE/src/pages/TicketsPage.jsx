import React, { useState, useEffect } from 'react';
import { Ticket, Download, CheckCircle, XCircle } from 'lucide-react';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';
import TicketCard from '../components/TicketCard';
import api from '../services/api';

const TicketsPage = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [downloading, setDownloading] = useState({});
  const [downloadingAll, setDownloadingAll] = useState(false);
  const [downloadSuccess, setDownloadSuccess] = useState(null);
  const [downloadError, setDownloadError] = useState(null);

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      const response = await api.get('/tickets');
      setTickets(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load tickets. Please try again.');
      console.error('Error fetching tickets:', err);
    } finally {
      setLoading(false);
    }
  };

  const classifyDownloadError = (err) => {
    if (err.code === 'ECONNABORTED') {
      return 'Request timed out. Please try again.';
    } else if (err.response?.status === 404) {
      return 'Ticket not found.';
    } else if (err.response?.status === 401) {
      return 'You are not authorized to download this ticket.';
    } else if (err.response?.status === 403) {
      return 'Access denied.';
    } else if (err.response?.status) {
      return `Server error: ${err.response.status}`;
    } else {
      return 'No response from server. Please check your connection.';
    }
  };

  const handleDownload = async (ticketId) => {
    if (downloading[ticketId]) return;

    setDownloading(prev => ({ ...prev, [ticketId]: true }));
    setDownloadSuccess(null);
    setDownloadError(null);

    try {
      const response = await api.get(`/tickets/${ticketId}/download`, {
        responseType: 'blob',
        timeout: 30000
      });

      const contentType = response.headers['content-type'];
      if (contentType && contentType.includes('application/pdf')) {
        const blob = new Blob([response.data], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;

        const contentDisposition = response.headers['content-disposition'];
        let filename = `ticket-${ticketId}.pdf`;

        if (contentDisposition) {
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].replace(/['"]/g, '');
          }
        }

        link.setAttribute('download', filename);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);

        setDownloadSuccess('Ticket downloaded successfully!');
        setTimeout(() => setDownloadSuccess(null), 3000);
      } else {
        throw new Error('Unexpected response type');
      }
    } catch (err) {
      console.error('Error downloading ticket:', err);
      const message = classifyDownloadError(err);
      setDownloadError(message);
      setTimeout(() => setDownloadError(null), 5000);
    } finally {
      setDownloading(prev => ({ ...prev, [ticketId]: false }));
    }
  };

  const handleDownloadAll = async () => {
    if (tickets.length === 0 || downloadingAll) return;

    setDownloadingAll(true);
    setDownloadError(null);

    try {
      // Group tickets by booking
      const bookings = {};
      tickets.forEach(ticket => {
        const bookingId = ticket.booking?.id || 'unknown';
        if (!bookings[bookingId]) {
          bookings[bookingId] = [];
        }
        bookings[bookingId].push(ticket);
      });

      // Download each booking's tickets as a batch
      for (const [bookingId, bookingTickets] of Object.entries(bookings)) {
        if (bookingTickets.length > 0) {
          try {
            const response = await api.get(`/tickets/booking/${bookingId}/download`, {
              responseType: 'blob',
              timeout: 30000
            });

            const blob = new Blob([response.data], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `booking-${bookingId}.pdf`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
          } catch (err) {
            console.error(`Error downloading booking ${bookingId}:`, err);
            const message = classifyDownloadError(err);
            setDownloadError(message);
            setTimeout(() => setDownloadError(null), 5000);
          }
        }
      }

      if (!downloadError) {
        setDownloadSuccess('All tickets downloaded successfully!');
        setTimeout(() => setDownloadSuccess(null), 3000);
      }
    } catch (err) {
      console.error('Error downloading all tickets:', err);
      setDownloadError('Failed to download all tickets.');
      setTimeout(() => setDownloadError(null), 5000);
    } finally {
      setDownloadingAll(false);
    }
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorAlert message={error} onClose={() => setError(null)} />;

  if (tickets.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="bg-gray-100 rounded-full w-24 h-24 flex items-center justify-center mx-auto mb-4">
          <Ticket className="w-12 h-12 text-gray-400" />
        </div>
        <h2 className="text-2xl font-bold text-gray-800 mb-2">No Tickets Yet</h2>
        <p className="text-gray-600 mb-6">When you purchase tickets, they'll appear here.</p>
        <a
          href="/events"
          className="inline-block bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Browse Events
        </a>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto">
      {/* Header with Download All button */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-8 gap-4">
        <h1 className="text-3xl font-bold text-gray-900">My Tickets</h1>
        {tickets.length > 0 && (
          <button
            onClick={handleDownloadAll}
            disabled={downloadingAll}
            className={`flex items-center space-x-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors ${downloadingAll ? 'opacity-50 cursor-not-allowed' : ''}`}
          >
            <Download className="w-4 h-4" />
            <span>{downloadingAll ? 'Downloading...' : `Download All (${tickets.length})`}</span>
          </button>
        )}
      </div>

      {/* Success/Error Messages */}
      {downloadSuccess && (
        <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg flex items-center text-green-700">
          <CheckCircle className="w-5 h-5 mr-2 flex-shrink-0" />
          <span>{downloadSuccess}</span>
        </div>
      )}

      {downloadError && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center text-red-700">
          <XCircle className="w-5 h-5 mr-2 flex-shrink-0" />
          <span>{downloadError}</span>
        </div>
      )}

      {/* Tickets Grid */}
      <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        {tickets.map(ticket => (
          <TicketCard
            key={ticket.id}
            ticket={{
              ...ticket,
              qrCode: ticket.qrSecret ? `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(ticket.qrSecret)}` : undefined
            }}
            onDownload={handleDownload}
            downloading={downloading[ticket.id]}
          />
        ))}
      </div>
    </div>
  );
};

export default TicketsPage;
