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

  const quickActions = [
    { to: '/tickets', icon: Ticket, title: 'My Tickets', copy: 'View and download your tickets' },
    { to: '/profile', icon: User, title: 'Profile Settings', copy: 'Update your personal information' },
    { to: '/events', icon: Calendar, title: 'Browse Events', copy: 'Discover new events near you' },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Header */}
      <div className="relative overflow-hidden border-8 border-black bg-black p-8 shadow-brutal-red-lg md:p-12">
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 bg-mx-dots-red mx-dots-sm opacity-60"
        />
        <div className="relative">
          <span className="mx-tag bg-red-700 text-white">Dashboard</span>
          <h1 className="mt-4 font-display uppercase tracking-tighter text-3xl md:text-5xl text-white">
            Welcome back, {user.fullName}!
          </h1>
          {user.memberSince && (
            <p className="mt-3 text-[11px] font-bold uppercase tracking-widest text-gray-300">
              Member since {user.memberSince}
            </p>
          )}
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid md:grid-cols-3 gap-6">
        {loading ? (
          <LoadingSpinner />
        ) : (
          <>
            {error && <ErrorAlert message={error} />}
            <div className="border-4 border-black bg-white p-6 shadow-brutal">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="font-display uppercase tracking-widest text-[11px] text-black">
                    Tickets Purchased
                  </p>
                  <p className="mt-2 font-display text-6xl leading-none text-black">{ticketCount}</p>
                </div>
                <div className="flex h-14 w-14 flex-shrink-0 items-center justify-center border-4 border-black bg-red-700 shadow-brutal-sm">
                  <Ticket className="w-6 h-6 text-white" />
                </div>
              </div>
              <div aria-hidden="true" className="mt-5 h-3 w-full bg-mx-stripes opacity-20" />
            </div>
          </>
        )}
      </div>

      {/* Quick Actions */}
      <div className="border-8 border-black bg-white shadow-brutal-lg">
        <div className="border-b-8 border-black bg-black px-6 py-5">
          <h2 className="font-display uppercase tracking-tighter text-2xl text-white">
            Quick Actions
          </h2>
        </div>

        <div className="grid md:grid-cols-3">
          {quickActions.map(({ to, icon: Icon, title, copy }) => (
            <Link
              key={to}
              to={to}
              className="group border-b-4 border-r-4 border-black p-6 transition-colors duration-150 last:border-r-0 hover:bg-red-700"
            >
              <div className="inline-flex h-12 w-12 items-center justify-center border-4 border-black bg-red-700 shadow-brutal-sm group-hover:bg-black">
                <Icon className="w-5 h-5 text-white" />
              </div>
              <h3 className="mt-4 font-display uppercase tracking-tight text-lg text-black group-hover:text-white">
                {title}
              </h3>
              <p className="mt-1 text-xs font-bold uppercase tracking-wide text-gray-600 group-hover:text-white">
                {copy}
              </p>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
