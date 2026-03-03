import React, { useState, useEffect } from 'react';
import { Ticket, Download } from 'lucide-react';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';
import TicketCard from '../components/TicketCard';
import api from '../services/api';

const TicketsPage = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async () => {
    try {
      setLoading(true);
      // Mock data for now - replace with actual API call
      const mockTickets = [
        {
          id: 1,
          ticketCode: 'EVT1-12345678',
          status: 'ACTIVE',
          pricePaid: 99.99,
          seatNumber: 'A12',
          ticketTier: { name: 'VIP' },
          booking: {
            event: {
              title: 'Summer Music Festival',
              startTime: '2024-07-15T19:00:00',
              venue: { name: 'City Arena' }
            }
          },
          qrCode: 'data:image/png;base64,test'
        }
      ];
      setTickets(mockTickets);
      setError(null);
    } catch (err) {
      setError('Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  const handleDownload = (ticketId) => {
    console.log('Downloading ticket:', ticketId);
    // Implement download logic
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorAlert message={error} />;

  if (tickets.length === 0) {
    return (
      <div className="text-center py-12">
        <Ticket className="w-16 h-16 text-gray-400 mx-auto mb-4" />
        <h2 className="text-2xl font-bold text-gray-800 mb-2">No Tickets Yet</h2>
        <p className="text-gray-600">When you purchase tickets, they'll appear here.</p>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-8">My Tickets</h1>
      <div className="grid md:grid-cols-2 gap-6">
        {tickets.map(ticket => (
          <TicketCard 
            key={ticket.id} 
            ticket={ticket} 
            onDownload={handleDownload}
          />
        ))}
      </div>
    </div>
  );
};

export default TicketsPage;