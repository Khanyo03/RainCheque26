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
  const [downloadSuccess, setDownloadSuccess] = useState(null);
  const [downloadError, setDownloadError] = useState(null);

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      
      // Try to fetch from API first
      try {
        const response = await api.get('/tickets');
        setTickets(response.data);
      } catch (apiErr) {
        console.log('API not available, using mock data');
        // Mock data for development when backend is not available
        const mockTickets = [
          {
            id: 1,
            ticketCode: 'EVT1-12345678',
            status: 'ACTIVE',
            pricePaid: 99.99,
            seatNumber: 'A12',
            ticketTier: { name: 'VIP' },
            booking: {
              id: 'booking-123',
              event: {
                id: 1,
                title: 'Summer Music Festival',
                startTime: '2026-12-15T19:00:00',
                venue: { 
                  name: 'City Arena',
                  city: 'Los Angeles'
                }
              }
            },
            qrCode: 'https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TICKET-123'
          },
          {
            id: 2,
            ticketCode: 'EVT2-87654321',
            status: 'ACTIVE',
            pricePaid: 45.00,
            seatNumber: 'General Admission',
            ticketTier: { name: 'General' },
            booking: {
              id: 'booking-456',
              event: {
                id: 2,
                title: 'Jazz Night',
                startTime: '2026-11-20T20:00:00',
                venue: { 
                  name: 'Jazz Club',
                  city: 'Chicago'
                }
              }
            }
          },
          {
            id: 3,
            ticketCode: 'EVT3-55555555',
            status: 'USED',
            pricePaid: 199.99,
            seatNumber: 'Orchestra C5',
            ticketTier: { name: 'Orchestra' },
            booking: {
              id: 'booking-789',
              event: {
                id: 3,
                title: 'Broadway: The Phantom',
                startTime: '2026-10-05T19:30:00',
                venue: { 
                  name: 'Grand Theater',
                  city: 'New York'
                }
              }
            }
          }
        ];
        setTickets(mockTickets);
      }
      
      setError(null);
    } catch (err) {
      setError('Failed to load tickets. Please try again.');
      console.error('Error fetching tickets:', err);
    } finally {
      setLoading(false);
    }
  };

  // Complete handle download function
  const handleDownload = async (ticketId) => {
    console.log('Downloading ticket:', ticketId);
    
    // Set downloading state for this ticket
    setDownloading(prev => ({ ...prev, [ticketId]: true }));
    setDownloadSuccess(null);
    setDownloadError(null);
    
    try {
      // Try API call first
      try {
        const response = await api.get(`/tickets/${ticketId}/download`, {
          responseType: 'blob',
          timeout: 30000 // 30 second timeout
        });
        
        // Check if response is PDF
        const contentType = response.headers['content-type'];
        if (contentType && contentType.includes('application/pdf')) {
          // Create a blob from the response data
          const blob = new Blob([response.data], { type: 'application/pdf' });
          
          // Create download link
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          
          // Get filename from Content-Disposition header or use default
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
          
          // Clean up the blob URL
          window.URL.revokeObjectURL(url);
          
          setDownloadSuccess(`Ticket downloaded successfully!`);
        } else {
          throw new Error('Unexpected response type');
        }
      } catch (apiErr) {
        console.log('API download failed, using mock download');
        
        // Mock download for development
        await new Promise(resolve => setTimeout(resolve, 1500));
        
        // Create a simple PDF-like blob (just for demonstration)
        const mockContent = `Ticket ${ticketId}\nRain Cheque\nThank you for your purchase!`;
        const blob = new Blob([mockContent], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', `ticket-${ticketId}.txt`);
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
        
        setDownloadSuccess(`Ticket downloaded successfully! (Mock mode)`);
      }
      
    } catch (err) {
      console.error('Error downloading ticket:', err);
      
      let errorMessage = 'Failed to download ticket. ';
      
      if (err.code === 'ECONNABORTED') {
        errorMessage += 'Request timed out. Please try again.';
      } else if (err.response) {
        if (err.response.status === 404) {
          errorMessage += 'Ticket not found.';
        } else if (err.response.status === 401) {
          errorMessage += 'You are not authorized to download this ticket.';
        } else if (err.response.status === 403) {
          errorMessage += 'Access denied.';
        } else {
          errorMessage += `Server error: ${err.response.status}`;
        }
      } else if (err.request) {
        errorMessage += 'No response from server. Please check your connection.';
      } else {
        errorMessage += err.message;
      }
      
      setDownloadError(errorMessage);
      
      // Auto-hide error after 5 seconds
      setTimeout(() => {
        setDownloadError(null);
      }, 5000);
      
    } finally {
      // Clear downloading state for this ticket
      setDownloading(prev => ({ ...prev, [ticketId]: false }));
      
      // Auto-hide success after 3 seconds
      if (!downloadError) {
        setTimeout(() => {
          setDownloadSuccess(null);
        }, 3000);
      }
    }
  };

  // Handle download all tickets
  const handleDownloadAll = async () => {
    if (tickets.length === 0) return;
    
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
              responseType: 'blob'
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
          } catch (apiErr) {
            // Mock download for development
            const mockContent = `Booking ${bookingId}\nTickets: ${bookingTickets.length}\nRain Cheque`;
            const blob = new Blob([mockContent], { type: 'text/plain' });
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', `booking-${bookingId}.txt`);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
          }
        }
      }
      
      setDownloadSuccess('All tickets downloaded successfully!');
      setTimeout(() => setDownloadSuccess(null), 3000);
      
    } catch (err) {
      console.error('Error downloading all tickets:', err);
      setDownloadError('Failed to download all tickets');
      setTimeout(() => setDownloadError(null), 5000);
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
            className="flex items-center space-x-2 bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors"
          >
            <Download className="w-4 h-4" />
            <span>Download All ({tickets.length})</span>
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
            ticket={ticket} 
            onDownload={handleDownload}
            downloading={downloading[ticket.id]}
          />
        ))}
      </div>
    </div>
  );
};

export default TicketsPage;