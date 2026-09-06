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
    <div className="relative bg-white rounded-none border-4 border-black shadow-brutal overflow-hidden transition-transform duration-150 hover:-translate-x-[3px] hover:-translate-y-[3px]">
      {/* Ticket Header */}
      <div className="relative bg-red-700 border-b-4 border-black p-4 overflow-hidden">
        <div
          className="absolute inset-0 bg-mx-dots-white mx-dots-sm opacity-40"
          aria-hidden="true"
        ></div>
        <h3 className="relative font-display uppercase tracking-tight text-2xl leading-tight text-white pr-8">
          {ticket.eventName || 'Event'}
        </h3>
        <p className="relative mt-2">
          <span className="mx-tag">{ticket.tierName}</span>
        </p>
      </div>

      <div className="p-6">
        {/* Status Badge */}
        <div className="flex justify-between items-start mb-4 gap-2">
          <span className={`px-3 py-1 border-2 border-black font-display uppercase tracking-wide text-xs flex items-center space-x-1 ${getStatusColor(ticket.status)}`}>
            {getStatusIcon(ticket.status)}
            <span>{ticket.status}</span>
          </span>
          <span className="text-sm font-mono bg-black text-white border-2 border-black px-2 py-1">
            #{ticket.ticketCode?.slice(-8)}
          </span>
        </div>

        {/* Ticket Details Grid */}
        <div className="grid grid-cols-2 gap-4 mb-4 text-sm">
          <div className="border-l-4 border-red-700 pl-2">
            <p className="text-gray-500 text-[10px] font-display uppercase tracking-widest">Date</p>
            <p className="font-bold uppercase">
              {hasValidDate ? format(startTime, 'MMM dd, yyyy') : 'TBA'}
            </p>
          </div>
          <div className="border-l-4 border-red-700 pl-2">
            <p className="text-gray-500 text-[10px] font-display uppercase tracking-widest">Time</p>
            <p className="font-bold uppercase">
              {hasValidDate ? format(startTime, 'hh:mm a') : '—'}
            </p>
          </div>
          <div className="border-l-4 border-red-700 pl-2">
            <p className="text-gray-500 text-[10px] font-display uppercase tracking-widest">Venue</p>
            <p className="font-bold uppercase truncate" title={ticket.venueName || ''}>
              {ticket.venueName || '—'}
            </p>
          </div>
          <div className="border-l-4 border-red-700 pl-2">
            <p className="text-gray-500 text-[10px] font-display uppercase tracking-widest">Seat</p>
            <p className="font-bold uppercase">{ticket.seatNumber || 'General Admission'}</p>
          </div>
        </div>
      </div>

      {/* Perforation divider with edge notches */}
      <div className="relative" aria-hidden="true">
        <div className="border-t-4 border-dashed border-black"></div>
        <div className="absolute -left-3 -top-3 h-6 w-6 rounded-full border-4 border-black bg-white"></div>
        <div className="absolute -right-3 -top-3 h-6 w-6 rounded-full border-4 border-black bg-white"></div>
      </div>

      <div className="p-6 pt-5">
        {/* Action Buttons */}
        <div className="flex space-x-3">
          <button
            onClick={() => onDownload(ticket.id)}
            disabled={downloading}
            className="mx-btn-black flex-1 px-4 py-2 text-sm"
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
            <button className="mx-btn-red px-3 py-2" aria-label="Show QR code">
              <QrCode className="w-5 h-5" />
            </button>
          )}
        </div>

        {/* Price */}
        <div className="mt-5 pt-4 border-t-4 border-black text-right">
          <span className="text-[10px] font-display uppercase tracking-widest text-gray-500">Price Paid: </span>
          <span className="font-display text-3xl leading-none text-black">${ticket.pricePaid}</span>
        </div>
      </div>
    </div>
  );
};

export default TicketCard;
