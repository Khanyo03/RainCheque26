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
      <div className="bg-white rounded-none border-4 border-black shadow-brutal overflow-hidden transition-all duration-150 hover:shadow-brutal-lg hover:-translate-x-[3px] hover:-translate-y-[3px] h-full flex flex-col">
        <div className="relative h-48 overflow-hidden bg-black border-b-4 border-black">
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
            <div className="relative w-full h-full bg-gradient-to-br from-red-700 to-black flex items-center justify-center">
              <div className="absolute inset-0 bg-mx-stripes opacity-30" aria-hidden="true"></div>
              <span className="relative font-display uppercase tracking-tight text-white text-2xl">Rain Cheque</span>
            </div>
          )}
          {event.featured && (
            <span className="absolute top-4 right-4 mx-sticker text-xs tracking-widest">
              Featured
            </span>
          )}
        </div>
        
        <div className="p-6 flex-1 flex flex-col">
          <h3 className="font-display uppercase tracking-tight text-2xl text-black mb-3 line-clamp-2">
            {event.title || 'Untitled Event'}
          </h3>
          
          <p className="text-gray-600 text-sm mb-4 line-clamp-2 flex-1">
            {event.shortDescription || event.description || 'No description available'}
          </p>

          <div className="mb-4 h-2 w-full bg-mx-stripes bg-black/10" aria-hidden="true"></div>

          <div className="space-y-2 text-xs font-bold uppercase tracking-wide text-black">
            <div className="flex items-center">
              <Calendar className="w-4 h-4 mr-2 text-red-700 flex-shrink-0" />
              <span className="truncate">{formatDate(event.startTime)}</span>
            </div>
            
            <div className="flex items-center">
              <Clock className="w-4 h-4 mr-2 text-red-700 flex-shrink-0" />
              <span className="truncate">{formatTime(event.startTime)}</span>
            </div>
            
            <div className="flex items-center">
              <MapPin className="w-4 h-4 mr-2 text-red-700 flex-shrink-0" />
              <span className="truncate">{event.venueName || 'Venue TBD'}</span>
            </div>
          </div>
          
          <div className="mt-4 pt-4 border-t-4 border-black flex justify-between items-center gap-2">
            <div className="border-4 border-black bg-white shadow-brutal-sm">
              <div className="h-2 w-full bg-red-700" aria-hidden="true"></div>
              <div className="px-3 py-1">
                <span className="font-display text-3xl leading-none text-black">
                  ${event.minPrice?.toFixed(2) || '0.00'}
                </span>
                {event.maxPrice > event.minPrice && (
                  <span className="text-gray-500 text-sm font-bold"> - ${event.maxPrice?.toFixed(2)}</span>
                )}
              </div>
            </div>
            <span className="mx-tag flex items-center">
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
