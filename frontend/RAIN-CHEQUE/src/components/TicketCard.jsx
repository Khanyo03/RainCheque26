import React from 'react';
import { format } from 'date-fns';
import { Download, QrCode, CheckCircle, XCircle, RefreshCw } from 'lucide-react';

const TicketCard = ({ ticket, onDownload, downloading }) => {
  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800 border-green-200';
      case 'USED': return 'bg-gray-100 text-gray-800 border-gray-200';
      case 'REFUNDED': return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'CANCELLED': return 'bg-red-100 text-red-800 border-red-200';
      default: return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'ACTIVE': return <CheckCircle className="w-4 h-4" />;
      case 'USED': return <CheckCircle className="w-4 h-4" />;
      case 'REFUNDED': return <RefreshCw className="w-4 h-4" />;
      case 'CANCELLED': return <XCircle className="w-4 h-4" />;
      default: return null;
    }
  };

  // Safe date parsing: the API may omit the event start time.
  const startTime = ticket.eventStartTime ? new Date(ticket.eventStartTime) : null;
  const hasValidDate = startTime && !Number.isNaN(startTime.getTime());

  return (
    <div className="bg-white rounded-xl shadow-lg overflow-hidden hover:shadow-xl transition-all duration-300 border-2 border-transparent hover:border-primary-200">
      {/* Ticket Header with Diagonal Cut Effect */}
      <div className="relative bg-gradient-to-r from-primary-600 to-primary-800 p-4">
        <div className="absolute right-0 top-0 w-16 h-16 bg-white transform rotate-45 translate-x-8 -translate-y-8"></div>
        <h3 className="text-lg font-bold text-white pr-8">
          {ticket.eventName || 'Event'}
        </h3>
        <p className="text-primary-100 text-sm mt-1">
          {ticket.tierName}
        </p>
      </div>

      <div className="p-6">
        {/* Status Badge */}
        <div className="flex justify-between items-start mb-4">
          <span className={`px-3 py-1 rounded-full text-xs font-medium flex items-center space-x-1 ${getStatusColor(ticket.status)}`}>
            {getStatusIcon(ticket.status)}
            <span>{ticket.status}</span>
          </span>
          <span className="text-sm font-mono bg-gray-100 px-2 py-1 rounded">
            #{ticket.ticketCode?.slice(-8)}
          </span>
        </div>

        {/* Ticket Details Grid */}
        <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
          <div>
            <p className="text-gray-500 text-xs">Date</p>
            <p className="font-medium">
              {hasValidDate ? format(startTime, 'MMM dd, yyyy') : 'TBA'}
            </p>
          </div>
          <div>
            <p className="text-gray-500 text-xs">Time</p>
            <p className="font-medium">
              {hasValidDate ? format(startTime, 'hh:mm a') : '—'}
            </p>
          </div>
          <div>
            <p className="text-gray-500 text-xs">Venue</p>
            <p className="font-medium truncate" title={ticket.venueName || ''}>
              {ticket.venueName || '—'}
            </p>
          </div>
          <div>
            <p className="text-gray-500 text-xs">Seat</p>
            <p className="font-medium">{ticket.seatNumber || 'General Admission'}</p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex space-x-3">
          <button
            onClick={() => onDownload(ticket.id)}
            disabled={downloading}
            className="flex-1 flex items-center justify-center space-x-2 bg-primary-600 text-white px-4 py-2 rounded-lg hover:bg-primary-700 transition-colors disabled:bg-primary-300"
          >
            {downloading ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                <span>Downloading...</span>
              </>
            ) : (
              <>
                <Download className="w-4 h-4" />
                <span>Download Ticket</span>
              </>
            )}
          </button>
          {ticket.status === 'ACTIVE' && (
            <button className="flex items-center justify-center p-2 border-2 border-primary-600 text-primary-600 rounded-lg hover:bg-primary-50 transition-colors">
              <QrCode className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Price */}
        <div className="mt-4 pt-4 border-t text-right">
          <span className="text-sm text-gray-500">Price Paid: </span>
          <span className="text-lg font-bold text-primary-600">${ticket.pricePaid}</span>
        </div>
      </div>
    </div>
  );
};

export default TicketCard;
