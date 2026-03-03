import React from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Ticket, User, Settings, Heart } from 'lucide-react';

const DashboardPage = () => {
  // Mock user data - replace with actual user data from context
  const user = {
    fullName: 'John Doe',
    email: 'john@example.com',
    memberSince: '2024',
    upcomingEvents: 3,
    pastEvents: 12
  };

  const stats = [
    { label: 'Upcoming Events', value: user.upcomingEvents, icon: Calendar, color: 'blue' },
    { label: 'Tickets Purchased', value: user.pastEvents, icon: Ticket, color: 'green' },
    { label: 'Wishlist', value: 5, icon: Heart, color: 'red' },
  ];

  return (
    <div>
      {/* Welcome Header */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 rounded-2xl p-8 text-white mb-8">
        <h1 className="text-3xl font-bold mb-2">Welcome back, {user.fullName}!</h1>
        <p className="text-blue-100">Member since {user.memberSince}</p>
      </div>

      {/* Stats Grid */}
      <div className="grid md:grid-cols-3 gap-6 mb-8">
        {stats.map((stat, index) => {
          const Icon = stat.icon;
          return (
            <div key={index} className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-gray-500 text-sm">{stat.label}</p>
                  <p className="text-3xl font-bold text-gray-900 mt-1">{stat.value}</p>
                </div>
                <div className={`bg-${stat.color}-100 p-3 rounded-lg`}>
                  <Icon className={`w-6 h-6 text-${stat.color}-600`} />
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Quick Actions */}
      <div className="bg-white rounded-xl shadow-md p-6">
        <h2 className="text-xl font-semibold mb-4">Quick Actions</h2>
        <div className="grid md:grid-cols-3 gap-4">
          <Link to="/tickets" className="p-4 border rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors">
            <Ticket className="w-6 h-6 text-blue-600 mb-2" />
            <h3 className="font-medium">My Tickets</h3>
            <p className="text-sm text-gray-500">View and download your tickets</p>
          </Link>
          
          <Link to="/profile" className="p-4 border rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors">
            <User className="w-6 h-6 text-blue-600 mb-2" />
            <h3 className="font-medium">Profile Settings</h3>
            <p className="text-sm text-gray-500">Update your personal information</p>
          </Link>
          
          <Link to="/events" className="p-4 border rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors">
            <Calendar className="w-6 h-6 text-blue-600 mb-2" />
            <h3 className="font-medium">Browse Events</h3>
            <p className="text-sm text-gray-500">Discover new events near you</p>
          </Link>
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;