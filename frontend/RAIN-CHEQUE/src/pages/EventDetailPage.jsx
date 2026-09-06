import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { 
  Calendar, Clock, MapPin, Users, Share2, Heart, 
  ChevronLeft, AlertCircle, Ticket, Info, Loader, CheckCircle
} from 'lucide-react';
import { format } from 'date-fns';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';
import api from '../services/api';

const EventDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedTier, setSelectedTier] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [bookingStep, setBookingStep] = useState('details'); // details, payment, confirmation

  useEffect(() => {
    fetchEventDetails();
  }, [id]);

  // Handle payment return from Stripe
  useEffect(() => {
    const paymentStatus = searchParams.get('payment');
    if (paymentStatus === 'success') {
      setBookingStep('confirmation');
    } else if (paymentStatus === 'cancelled') {
      setError('Payment was cancelled. You can try again when ready.');
    }
  }, [searchParams]);

  const fetchEventDetails = async () => {
    if (!id || !/^\d+$/.test(id)) {
      setError('Invalid event ID');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      const response = await api.get(`/events/${id}`);
      setEvent(response.data);
      // Only clear errors from fetching, not from payment return
      if (!searchParams.get('payment')) {
        setError(null);
      }
    } catch (err) {
      if (err.response?.status === 404) {
        setError('Event not found');
      } else {
        setError('Failed to load event details. Please try again.');
      }
      console.error('Error fetching event:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleBookNow = async () => {
    if (!selectedTier) return;
    try {
      setLoading(true);
      setError(null);
      setBookingStep('payment');

      const user = JSON.parse(localStorage.getItem('user') || '{}');

      const paymentData = {
        eventId: event.id,
        ticketId: selectedTier.id,
        amount: selectedTier.price * quantity,
        currency: 'ZAR',
        successUrl: `${window.location.origin}/tickets?payment=success`,
        cancelUrl: `${window.location.origin}/events/${event.id}?payment=cancelled`,
        customerEmail: user.email || ''
      };

      const response = await api.post('/payments/checkout', paymentData);

      if (response.data.checkoutUrl) {
        window.location.href = response.data.checkoutUrl;
      } else {
        setBookingStep('details');
        setError('Unable to create checkout session. Please try again.');
      }
    } catch (err) {
      setBookingStep('details');
      if (err.response?.status === 503) {
        setError('Payment provider is temporarily unavailable. Please try again later.');
      } else if (err.response?.status === 400) {
        setError('Invalid payment request. Please check your selection and try again.');
      } else if (err.response?.status === 401) {
        setError('Please log in to purchase tickets.');
      } else {
        setError('Failed to initiate payment. Please try again.');
      }
      console.error('Payment error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading && !event) return <LoadingSpinner fullPage />;
  if (error && !event) return <ErrorAlert message={error} onClose={() => setError(null)} />;
  if (!event) return <div>Event not found</div>;

  return (
    <div className="max-w-7xl mx-auto">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="inline-flex items-center gap-1 border-4 border-black bg-white px-4 py-2 font-display text-xs uppercase tracking-widest text-black shadow-brutal-sm mb-6 transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] hover:text-red-700 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
      >
        <ChevronLeft className="w-4 h-4 mr-1" />
        Back to Events
      </button>

      {/* Event Header with Image */}
      <div className="relative h-96 overflow-hidden mb-8 border-8 border-black shadow-brutal-lg">
        <img
          src={event.bannerImageUrl}
          alt={event.title}
          className="w-full h-full object-cover"
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80';
          }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black via-black/70 to-black/10"></div>
        <div className="absolute inset-x-0 bottom-0 h-2 bg-red-700"></div>
        <div className="absolute bottom-0 left-0 right-0 p-6 md:p-8 text-white">
          <h1 className="font-display uppercase tracking-tighter leading-[0.9] text-4xl md:text-6xl mb-4">{event.title}</h1>
          <div className="flex items-center space-x-4">
            <span className="mx-tag border-2 border-white bg-black text-white">
              {event.category}
            </span>
            {event.featured && (
              <span className="mx-sticker text-xs tracking-widest animate-wiggle">
                Featured
              </span>
            )}
          </div>
        </div>
      </div>

      <div className="grid md:grid-cols-3 gap-8">
        {/* Left Column - Event Details */}
        <div className="md:col-span-2 space-y-6">
          {/* Quick Info Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="border-4 border-black bg-white p-4 shadow-brutal-sm">
              <Calendar className="w-5 h-5 text-red-700 mb-2" />
              <p className="font-display uppercase tracking-widest text-[11px] text-black">Date</p>
              <p className="font-bold">{format(new Date(event.startTime), 'MMM dd, yyyy')}</p>
            </div>
            <div className="border-4 border-black bg-white p-4 shadow-brutal-sm">
              <Clock className="w-5 h-5 text-red-700 mb-2" />
              <p className="font-display uppercase tracking-widest text-[11px] text-black">Time</p>
              <p className="font-bold">
                {format(new Date(event.startTime), 'hh:mm a')}
              </p>
            </div>
            <div className="border-4 border-black bg-white p-4 shadow-brutal-sm">
              <MapPin className="w-5 h-5 text-red-700 mb-2" />
              <p className="font-display uppercase tracking-widest text-[11px] text-black">Venue</p>
              <p className="font-bold">{event.venueName}</p>
            </div>
            <div className="border-4 border-black bg-white p-4 shadow-brutal-sm">
              <Users className="w-5 h-5 text-red-700 mb-2" />
              <p className="font-display uppercase tracking-widest text-[11px] text-black">Capacity</p>
              <p className="font-bold">{event.venue?.capacity || 'N/A'}</p>
            </div>
          </div>

          {/* Description */}
          <div className="border-4 border-black bg-white shadow-brutal">
            <div className="bg-black px-6 py-3">
              <h2 className="font-display uppercase tracking-widest text-white text-lg">About This Event</h2>
            </div>
            <div className="p-6">
              <p className="text-gray-800 whitespace-pre-line">{event.description}</p>
            </div>
          </div>

          {/* Venue Information */}
          <div className="border-4 border-black bg-white shadow-brutal">
            <div className="bg-black px-6 py-3">
              <h2 className="font-display uppercase tracking-widest text-white text-lg">Venue Information</h2>
            </div>
            <div className="p-6">
              <div className="space-y-2">
                <p className="flex items-start font-medium">
                  <MapPin className="w-5 h-5 text-red-700 mr-2 mt-0.5 flex-shrink-0" />
                  <span>{event.venue?.address || event.venueAddress}, {event.venue?.city || event.venueCity}, {event.venue?.country || 'USA'}</span>
                </p>
                {event.venue?.contactEmail && (
                  <p className="text-sm text-gray-700">Email: {event.venue.contactEmail}</p>
                )}
                {event.venue?.contactPhone && (
                  <p className="text-sm text-gray-700">Phone: {event.venue.contactPhone}</p>
                )}
              </div>
            </div>
          </div>

          {/* Weather Policy */}
          {event.metadata?.weatherPolicy && (
            <div className="border-4 border-black bg-black p-6 text-white shadow-brutal-red">
              <div className="flex items-start">
                <span className="mr-4 flex h-10 w-10 flex-shrink-0 items-center justify-center border-2 border-white bg-white">
                  <Info className="w-5 h-5 text-red-700" />
                </span>
                <div>
                  <h3 className="font-display uppercase tracking-widest text-white mb-2">Rain Cheque Policy</h3>
                  <p className="text-gray-200">
                    This event is covered by our Rain Cheque guarantee. If the event is 
                    cancelled due to weather, you'll receive a full refund or 120% site credit.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Right Column - Booking Section */}
        <div className="md:col-span-1">
          <div className="border-8 border-black bg-white shadow-brutal-lg sticky top-24">
            <div className="bg-black px-6 py-4">
              <h2 className="font-display uppercase tracking-widest text-white text-2xl">Get Tickets</h2>
            </div>

            <div className="p-6">
            {error && (
              <div className="mb-4 border-4 border-black bg-red-700 p-3 text-white shadow-brutal">
                <div className="flex items-start">
                  <AlertCircle className="w-4 h-4 text-white mr-2 mt-0.5 flex-shrink-0" />
                  <p className="text-sm font-bold text-white">{error}</p>
                </div>
              </div>
            )}
            
            {bookingStep === 'details' && (
              <>
                {/* Ticket Tier Selection */}
                <div className="space-y-4 mb-6">
                  {event.ticketTiers?.map(tier => (
                    <div
                      key={tier.id}
                      className={`border-4 p-4 cursor-pointer transition-all ${
                        selectedTier?.id === tier.id
                          ? 'border-black bg-red-700 text-white shadow-brutal'
                          : 'border-black hover:bg-gray-100 shadow-brutal-sm'
                      }`}
                      onClick={() => setSelectedTier(tier)}
                    >
                      <div className="flex justify-between items-start mb-2 gap-2">
                        <div>
                          <h3 className={`font-display uppercase tracking-tight ${selectedTier?.id === tier.id ? 'text-white' : 'text-black'}`}>{tier.name}</h3>
                          <p className={`text-sm ${selectedTier?.id === tier.id ? 'text-white/80' : 'text-gray-600'}`}>{tier.description}</p>
                        </div>
                        <span className={`text-3xl font-display leading-none tracking-tighter ${selectedTier?.id === tier.id ? 'text-white' : 'text-black'}`}>
                          R{tier.price}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-sm">
                        <span className={`${tier.remainingQuantity < 50 ? 'text-red-700 font-bold' : (selectedTier?.id === tier.id ? 'text-white/80' : 'text-gray-600')}`}>
                          {tier.remainingQuantity} seats left
                        </span>
                        <span className={`${selectedTier?.id === tier.id ? 'text-white/80' : 'text-gray-600'}`}>
                          Max {tier.maxPerUser} per person
                        </span>
                      </div>
                      {tier.benefits?.length > 0 && (
                        <div className={`mt-3 pt-3 border-t-4 ${selectedTier?.id === tier.id ? 'border-white' : 'border-black'}`}>
                          <p className={`font-display text-[10px] uppercase tracking-widest mb-1 ${selectedTier?.id === tier.id ? 'text-white' : 'text-black'}`}>Includes:</p>
                          <ul className="text-xs space-y-1">
                            {tier.benefits.map((benefit, i) => (
                              <li key={i} className={`flex items-center font-medium ${selectedTier?.id === tier.id ? 'text-white' : 'text-gray-700'}`}>
                                <span className={`w-2 h-2 mr-2 flex-shrink-0 ${selectedTier?.id === tier.id ? 'bg-white' : 'bg-red-700'}`}></span>
                                {benefit}
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                {/* Quantity Selector */}
                {selectedTier && (
                  <div className="mb-6">
                    <label className="mx-label">
                      Quantity
                    </label>
                    <div className="flex items-center space-x-3">
                      <button
                        onClick={() => setQuantity(q => Math.max(1, q - 1))}
                        className="w-10 h-10 border-4 border-black bg-white font-display text-lg leading-none text-black shadow-brutal-sm transition-transform duration-100 hover:bg-gray-100 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none disabled:opacity-50 disabled:pointer-events-none"
                        disabled={quantity <= 1}
                      >
                        -
                      </button>
                      <span className="w-16 text-center font-display text-2xl tracking-tighter">{quantity}</span>
                      <button
                        onClick={() => setQuantity(q => Math.min(selectedTier.maxPerUser, q + 1))}
                        className="w-10 h-10 border-4 border-black bg-white font-display text-lg leading-none text-black shadow-brutal-sm transition-transform duration-100 hover:bg-gray-100 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none disabled:opacity-50 disabled:pointer-events-none"
                        disabled={quantity >= selectedTier.maxPerUser}
                      >
                        +
                      </button>
                    </div>
                  </div>
                )}

                {/* Price Summary */}
                {selectedTier && (
                  <div className="border-t-4 border-black pt-4 mb-6">
                    <div className="flex justify-between mb-2 text-sm font-medium">
                      <span>Price per ticket</span>
                      <span>R{selectedTier.price}</span>
                    </div>
                    <div className="flex justify-between mb-3 text-sm font-medium">
                      <span>Quantity</span>
                      <span>{quantity}</span>
                    </div>
                    <div className="flex items-baseline justify-between">
                      <span className="font-display uppercase tracking-widest text-sm">Total</span>
                      <span className="font-display text-3xl leading-none tracking-tighter text-black">
                        R{(selectedTier.price * quantity).toFixed(2)}
                      </span>
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <button
                  onClick={handleBookNow}
                  disabled={!selectedTier || loading}
                  className="w-full mb-3 inline-flex items-center justify-center gap-2 border-4 border-black bg-red-700 px-6 py-3 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none"
                >
                  {loading ? (
                    <>
                      <Loader className="w-4 h-4 animate-spin mr-2" />
                      Processing...
                    </>
                  ) : (
                    'Proceed to Payment'
                  )}
                </button>
                
                <button className="w-full inline-flex items-center justify-center space-x-2 border-4 border-black bg-white px-6 py-3 font-display uppercase tracking-tight text-black shadow-brutal-sm transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] hover:bg-gray-100 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none">
                  <Heart className="w-4 h-4" />
                  <span>Save to Wishlist</span>
                </button>
              </>
            )}

            {bookingStep === 'payment' && (
              <div className="text-center py-8">
                <Loader className="w-8 h-8 animate-spin text-red-700 mx-auto mb-4" />
                <p className="font-display uppercase tracking-widest text-black mb-2">Redirecting to secure payment...</p>
                <p className="text-sm text-gray-700">You'll be taken to Paystack's secure checkout page.</p>
              </div>
            )}

            {bookingStep === 'confirmation' && (
              <div className="text-center py-6">
                <div className="w-16 h-16 border-4 border-red-700 bg-black flex items-center justify-center mx-auto mb-4 shadow-brutal-red">
                  <CheckCircle className="w-8 h-8 text-white" />
                </div>
                <h3 className="font-display uppercase tracking-tight text-2xl text-black mb-2">Payment Successful!</h3>
                <div className="mx-auto mb-4 h-2 w-24 bg-red-700"></div>
                <p className="text-gray-700 mb-4">
                  Your tickets are being generated and will be sent to your email shortly.
                </p>
                <button
                  onClick={() => navigate('/tickets')}
                  className="w-full inline-flex items-center justify-center gap-2 border-4 border-black bg-black px-6 py-3 font-display uppercase tracking-tight text-white shadow-brutal-red transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
                >
                  View My Tickets
                </button>
              </div>
            )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetailPage;
