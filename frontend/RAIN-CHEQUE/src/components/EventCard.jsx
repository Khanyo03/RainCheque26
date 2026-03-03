import React from 'react';
import { Link } from 'react-router-dom';
import { Calendar, MapPin, Clock, Users } from 'lucide-react';

const EventCard = ({ event }) => {
  // Safely format date
  const formatDate = (dateString) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    } catch (e) {
      return 'Date TBD';
    }
  };

  const formatTime = (dateString) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    } catch (e) {
      return 'Time TBD';
    }
  };

  // Calculate total remaining tickets safely
  const getTotalRemaining = () => {
    if (!event.ticketTiers || !Array.isArray(event.ticketTiers)) return 0;
    return event.ticketTiers.reduce((acc, tier) => acc + (tier.remainingQuantity || 0), 0);
  };

  return (
    <Link to={`/events/${event.id}`} className="block h-full">
      <div className="bg-white rounded-xl shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 h-full flex flex-col">
        <div className="relative h-48 overflow-hidden bg-gray-200">
          {event.bannerImageUrl ? (
            <img
              src={event.bannerImageUrl}
              alt={event.title || 'Event'}
              className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
              onError={(e) => {
                e.target.onerror = null;
                e.target.src = 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80';
              }}
            />
          ) : (
            <div className="w-full h-full bg-gradient-to-r from-blue-400 to-blue-600 flex items-center justify-center">
              <span className="text-white text-lg font-bold">Rain Cheque</span>
            </div>
          )}
          {event.featured && (
            <span className="absolute top-4 right-4 bg-yellow-400 text-yellow-900 px-3 py-1 rounded-full text-sm font-semibold">
              Featured
            </span>
          )}
        </div>
        
        <div className="p-6 flex-1 flex flex-col">
          <h3 className="text-xl font-bold text-gray-900 mb-2 line-clamp-2">
            {event.title || 'Untitled Event'}
          </h3>
          
          <p className="text-gray-600 text-sm mb-4 line-clamp-2 flex-1">
            {event.shortDescription || event.description || 'No description available'}
          </p>
          
          <div className="space-y-2 text-sm text-gray-600">
            <div className="flex items-center">
              <Calendar className="w-4 h-4 mr-2 text-blue-600 flex-shrink-0" />
              <span className="truncate">{formatDate(event.startTime)}</span>
            </div>
            
            <div className="flex items-center">
              <Clock className="w-4 h-4 mr-2 text-blue-600 flex-shrink-0" />
              <span className="truncate">{formatTime(event.startTime)}</span>
            </div>
            
            <div className="flex items-center">
              <MapPin className="w-4 h-4 mr-2 text-blue-600 flex-shrink-0" />
              <span className="truncate">{event.venueName || 'Venue TBD'}</span>
            </div>
          </div>
          
          <div className="mt-4 pt-4 border-t flex justify-between items-center">
            <div>
              <span className="text-2xl font-bold text-blue-600">
                ${event.minPrice?.toFixed(2) || '0.00'}
              </span>
              {event.maxPrice > event.minPrice && (
                <span className="text-gray-500 text-sm"> - ${event.maxPrice?.toFixed(2)}</span>
              )}
            </div>
            <span className="text-sm text-gray-500 flex items-center">
              <Users className="w-4 h-4 mr-1" />
              {getTotalRemaining()} left
            </span>
          </div>
        </div>
      </div>
    </Link>
  );
};

export default EventCard;