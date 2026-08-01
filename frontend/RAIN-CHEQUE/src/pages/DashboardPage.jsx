import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Calendar, Ticket, User } from 'lucide-react';
import api from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';

const DashboardPage = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [ticketCount, setTicketCount] = useState(0);

  useEffect(() => {
    // Read and validate user from localStorage
    const raw = localStorage.getItem('user');
    if (!raw || raw.trim() === '') {
      navigate('/login');
      return;
    }
    try {
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        navigate('/login');
        return;
      }
      setUser(parsed);
    } catch {
      navigate('/login');
      return;
    }

    // Fetch ticket count from API
    const fetchTickets = async () => {
      try {
        const response = await api.get('/tickets');
        setTicketCount(response.data.length);
      } catch (err) {
        setError('Failed to load ticket data.');
        setTicketCount(0);
      } finally {
        setLoading(false);
      }
    };

    fetchTickets();
  }, [navigate]);

  if (!user) {
    return null;
  }

  return (
    <div>
      {/* Welcome Header */}
      <div className="bg-gradient-to-r from-white to-black rounded-2xl p-8 text-black mb-8">
        <h1 className="text-3xl font-bold mb-2">Welcome back, {user.fullName}!</h1>
        {user.memberSince && (
          <p className="text-gray-500">Member since {user.memberSince}</p>
        )}
      </div>

      {/* Stats Grid */}
      <div className="grid md:grid-cols-3 gap-6 mb-8">
        {loading ? (
          <LoadingSpinner />
        ) : (
          <>
            {error && <ErrorAlert message={error} />}
            <div className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-gray-500 text-sm">Tickets Purchased</p>
                  <p className="text-3xl font-bold text-gray-900 mt-1">{ticketCount}</p>
                </div>
                <div className="bg-black-100 p-3 rounded-lg">
                  <Ticket className="w-6 h-6 text-black-600" />
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-xl shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid md:grid-cols-3 gap-4">
          <Link to="/tickets" className="p-4 border-2 border-black rounded-lg hover:border-red-500 hover:bg-red-50 transition-colors">
            <Ticket className="w-6 h-6 text-red-700 mb-2" />
            <h3 className="font-medium">My Tickets</h3>
            <p className="text-sm text-gray-500">View and download your tickets</p>
          </Link>
          
          <Link to="/profile" className="p-4 border-2 border-black rounded-lg hover:border-red-500 hover:bg-red-50 transition-colors">
            <User className="w-6 h-6 text-red-700 mb-2" />
            <h3 className="font-medium">Profile Settings</h3>
            <p className="text-sm text-gray-500">Update your personal information</p>
          </Link>
          
          <Link to="/events" className="p-4 border-2 border-black rounded-lg hover:border-red-500 hover:bg-red-50 transition-colors">
            <Calendar className="w-6 h-6 text-red-700 mb-2" />
            <h3 className="font-medium">Browse Events</h3>
            <p className="text-sm text-gray-500">Discover new events near you</p>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
