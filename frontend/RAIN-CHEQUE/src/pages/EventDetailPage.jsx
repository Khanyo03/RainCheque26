import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { 
  Calendar, Clock, MapPin, Users, Share2, Heart, 
  ChevronLeft, AlertCircle, Ticket, Info, Loader
} from 'lucide-react';
import { format } from 'date-fns';
import LoadingSpinner from '../components/LoadingSpinner';
import ErrorAlert from '../components/ErrorAlert';
import api from '../services/api';

const EventDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [event, setEvent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedTier, setSelectedTier] = useState(null);
  const [quantity, setQuantity] = useState(1);
  const [booking, setBooking] = useState(null);
  const [bookingStep, setBookingStep] = useState('details'); // details, payment, confirmation
  const [usingMockData, setUsingMockData] = useState(false);

  // Mock event data for when backend is unavailable
  const mockEvents = {
    1: {
      id: 1,
      title: "Summer Music Festival",
      description: "A day of amazing live music performances featuring top artists from around the world. Food trucks, drinks, and unforgettable experiences await you at the biggest music festival of the summer!",
      shortDescription: "Live music festival",
      venueName: "City Arena",
      venueCity: "Los Angeles",
      venueAddress: "456 Sports Blvd, Los Angeles, CA 90001",
      venue: {
        name: "City Arena",
        city: "Los Angeles",
        address: "456 Sports Blvd",
        country: "USA",
        capacity: 5000,
        contactEmail: "info@cityarena.com",
        contactPhone: "+1 (555) 123-4567"
      },
      startTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
      endTime: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000 + 8 * 60 * 60 * 1000).toISOString(),
      bannerImageUrl: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
      category: "MUSIC",
      featured: true,
      metadata: {
        weatherPolicy: "standard",
        ageRestriction: 18,
        parkingAvailable: true,
        foodAllowed: true
      },
      ticketTiers: [
        {
          id: 1,
          name: "VIP",
          description: "Front row access + drinks",
          price: 299.99,
          remainingQuantity: 45,
          totalQuantity: 100,
          maxPerUser: 4,
          benefits: ["Front rows access", "Free drinks", "Meet & greet", "Exclusive lounge"]
        },
        {
          id: 2,
          name: "General",
          description: "Standard admission",
          price: 99.99,
          remainingQuantity: 320,
          totalQuantity: 500,
          maxPerUser: 10,
          benefits: ["General admission", "Access to all stages"]
        },
        {
          id: 3,
          name: "Early Bird",
          description: "Limited early discount",
          price: 79.99,
          remainingQuantity: 0,
          totalQuantity: 200,
          maxPerUser: 6,
          benefits: ["General admission", "Early entry", "Discount voucher"]
        }
      ]
    },
    2: {
      id: 2,
      title: "Broadway: The Phantom",
      description: "Experience the magic of Broadway with this classic musical performance of The Phantom of the Opera. A night of unforgettable music and theatrical excellence.",
      shortDescription: "Evening with Phantom",
      venueName: "Grand Theater",
      venueCity: "New York",
      venueAddress: "123 Broadway, New York, NY 10001",
      venue: {
        name: "Grand Theater",
        city: "New York",
        address: "123 Broadway",
        country: "USA",
        capacity: 1500,
        contactEmail: "contact@grandtheater.com",
        contactPhone: "+1 (555) 987-6543"
      },
      startTime: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000).toISOString(),
      endTime: new Date(Date.now() + 15 * 24 * 60 * 60 * 1000 + 3 * 60 * 60 * 1000).toISOString(),
      bannerImageUrl: "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
      category: "THEATRE",
      featured: true,
      metadata: {
        weatherPolicy: "indoor",
        ageRestriction: 0,
        parkingAvailable: true,
        foodAllowed: false
      },
      ticketTiers: [
        {
          id: 4,
          name: "Orchestra",
          description: "Best seats in house",
          price: 199.99,
          remainingQuantity: 85,
          totalQuantity: 150,
          maxPerUser: 6,
          benefits: ["Center orchestra", "Program included", "Best view"]
        },
        {
          id: 5,
          name: "Balcony",
          description: "Upper level seating",
          price: 89.99,
          remainingQuantity: 120,
          totalQuantity: 200,
          maxPerUser: 8,
          benefits: ["Balcony seating", "Good view"]
        }
      ]
    },
    3: {
      id: 3,
      title: "Jazz Night",
      description: "Smooth jazz evening with local artists. Enjoy an intimate night of world-class jazz performances in a cozy club setting.",
      shortDescription: "Jazz performances",
      venueName: "Jazz Club",
      venueCity: "Chicago",
      venueAddress: "789 Music Lane, Chicago, IL 60601",
      venue: {
        name: "Jazz Club",
        city: "Chicago",
        address: "789 Music Lane",
        country: "USA",
        capacity: 300,
        contactEmail: "bookings@jazzclub.com",
        contactPhone: "+1 (555) 456-7890"
      },
      startTime: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      endTime: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000 + 4 * 60 * 60 * 1000).toISOString(),
      bannerImageUrl: "https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80",
      category: "MUSIC",
      featured: false,
      metadata: {
        weatherPolicy: "standard",
        ageRestriction: 21,
        parkingAvailable: false,
        foodAllowed: true
      },
      ticketTiers: [
        {
          id: 6,
          name: "General",
          description: "General admission",
          price: 45.00,
          remainingQuantity: 150,
          totalQuantity: 300,
          maxPerUser: 10,
          benefits: ["General admission", "Live jazz", "One drink included"]
        }
      ]
    }
  };

  useEffect(() => {
    fetchEventDetails();
  }, [id]);

  const fetchEventDetails = async () => {
    try {
      setLoading(true);
      
      // Try to fetch from API first
      try {
        const response = await api.get(`/events/${id}`);
        setEvent(response.data);
        setUsingMockData(false);
        console.log('Event loaded from API:', response.data);
      } catch (apiErr) {
        console.log('API not available, using mock data for event ID:', id);
        
        // Use mock data based on ID
        const mockEvent = mockEvents[id] || mockEvents[1]; // Fallback to first event
        
        if (mockEvent) {
          setEvent(mockEvent);
          setUsingMockData(true);
        } else {
          throw new Error('Event not found');
        }
      }
      
      setError(null);
    } catch (err) {
      setError('Failed to load event details. Please try again.');
      console.error('Error fetching event:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleBookNow = async () => {
    if (!selectedTier) return;
    
    try {
      setBookingStep('payment');
      
      // Try API call first
      try {
        const response = await api.post('/bookings/initiate', {
          eventId: event.id,
          ticketTierId: selectedTier.id,
          quantity
        });
        setBooking(response.data);
      } catch (apiErr) {
        console.log('Using mock booking');
        // Mock booking response
        setBooking({
          id: `booking-${Date.now()}`,
          totalAmount: selectedTier.price * quantity
        });
      }
    } catch (err) {
      setError('Failed to initialize booking');
      console.error('Booking error:', err);
    }
  };

  const handlePayment = async () => {
    try {
      setLoading(true);
      
      // Try API call first
      try {
        const response = await api.post('/payments/confirm', {
          bookingId: booking.id
        });
        
        if (response.data.success) {
          setBookingStep('confirmation');
        }
      } catch (apiErr) {
        console.log('Using mock payment');
        // Mock successful payment
        setTimeout(() => {
          setBookingStep('confirmation');
        }, 1500);
      }
      
    } catch (err) {
      setError('Payment failed');
      console.error('Payment error:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <LoadingSpinner fullPage />;
  if (error) return <ErrorAlert message={error} onClose={() => setError(null)} />;
  if (!event) return <div>Event not found</div>;

  return (
    <div className="max-w-7xl mx-auto">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center text-gray-600 hover:text-blue-600 mb-6 transition-colors"
      >
        <ChevronLeft className="w-4 h-4 mr-1" />
        Back to Events
      </button>

      {/* Mock Data Indicator */}
      {usingMockData && (
        <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-yellow-700 text-sm flex items-center">
          <Info className="w-4 h-4 mr-2" />
          Using demo data - Backend not connected
        </div>
      )}

      {/* Event Header with Image */}
      <div className="relative h-96 rounded-2xl overflow-hidden mb-8">
        <img
          src={event.bannerImageUrl}
          alt={event.title}
          className="w-full h-full object-cover"
          onError={(e) => {
            e.target.onerror = null;
            e.target.src = 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?ixlib=rb-1.2.1&auto=format&fit=crop&w=1050&q=80';
          }}
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent"></div>
        <div className="absolute bottom-0 left-0 right-0 p-8 text-white">
          <h1 className="text-4xl font-bold mb-2">{event.title}</h1>
          <div className="flex items-center space-x-4">
            <span className="bg-blue-600 px-3 py-1 rounded-full text-sm">
              {event.category}
            </span>
            {event.featured && (
              <span className="bg-yellow-400 text-yellow-900 px-3 py-1 rounded-full text-sm">
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
            <div className="bg-white p-4 rounded-lg shadow-sm">
              <Calendar className="w-5 h-5 text-blue-600 mb-2" />
              <p className="text-sm text-gray-600">Date</p>
              <p className="font-semibold">{format(new Date(event.startTime), 'MMM dd, yyyy')}</p>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm">
              <Clock className="w-5 h-5 text-blue-600 mb-2" />
              <p className="text-sm text-gray-600">Time</p>
              <p className="font-semibold">
                {format(new Date(event.startTime), 'hh:mm a')}
              </p>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm">
              <MapPin className="w-5 h-5 text-blue-600 mb-2" />
              <p className="text-sm text-gray-600">Venue</p>
              <p className="font-semibold">{event.venueName}</p>
            </div>
            <div className="bg-white p-4 rounded-lg shadow-sm">
              <Users className="w-5 h-5 text-blue-600 mb-2" />
              <p className="text-sm text-gray-600">Capacity</p>
              <p className="font-semibold">{event.venue?.capacity || 'N/A'}</p>
            </div>
          </div>

          {/* Description */}
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <h2 className="text-xl font-semibold mb-4">About This Event</h2>
            <p className="text-gray-700 whitespace-pre-line">{event.description}</p>
          </div>

          {/* Venue Information */}
          <div className="bg-white p-6 rounded-lg shadow-sm">
            <h2 className="text-xl font-semibold mb-4">Venue Information</h2>
            <div className="space-y-2">
              <p className="flex items-start">
                <MapPin className="w-5 h-5 text-blue-600 mr-2 mt-0.5 flex-shrink-0" />
                <span>{event.venue?.address || event.venueAddress}, {event.venue?.city || event.venueCity}, {event.venue?.country || 'USA'}</span>
              </p>
              {event.venue?.contactEmail && (
                <p className="text-sm text-gray-600">Email: {event.venue.contactEmail}</p>
              )}
              {event.venue?.contactPhone && (
                <p className="text-sm text-gray-600">Phone: {event.venue.contactPhone}</p>
              )}
            </div>
          </div>

          {/* Weather Policy */}
          {event.metadata?.weatherPolicy && (
            <div className="bg-blue-50 p-6 rounded-lg border border-blue-200">
              <div className="flex items-start">
                <Info className="w-5 h-5 text-blue-600 mr-3 mt-0.5 flex-shrink-0" />
                <div>
                  <h3 className="font-semibold text-blue-800 mb-1">Rain Cheque Policy</h3>
                  <p className="text-blue-700">
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
          <div className="bg-white rounded-lg shadow-lg p-6 sticky top-24">
            <h2 className="text-2xl font-bold mb-4">Get Tickets</h2>
            
            {bookingStep === 'details' && (
              <>
                {/* Ticket Tier Selection */}
                <div className="space-y-4 mb-6">
                  {event.ticketTiers?.map(tier => (
                    <div
                      key={tier.id}
                      className={`border-2 rounded-lg p-4 cursor-pointer transition-all ${
                        selectedTier?.id === tier.id
                          ? 'border-blue-600 bg-blue-50'
                          : 'border-gray-200 hover:border-blue-300'
                      }`}
                      onClick={() => setSelectedTier(tier)}
                    >
                      <div className="flex justify-between items-start mb-2">
                        <div>
                          <h3 className="font-semibold">{tier.name}</h3>
                          <p className="text-sm text-gray-600">{tier.description}</p>
                        </div>
                        <span className="text-2xl font-bold text-blue-600">
                          ${tier.price}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-sm">
                        <span className={`${tier.remainingQuantity < 50 ? 'text-orange-600 font-medium' : 'text-gray-500'}`}>
                          {tier.remainingQuantity} seats left
                        </span>
                        <span className="text-gray-500">
                          Max {tier.maxPerUser} per person
                        </span>
                      </div>
                      {tier.benefits?.length > 0 && (
                        <div className="mt-2 pt-2 border-t">
                          <p className="text-xs text-gray-500 mb-1">Includes:</p>
                          <ul className="text-xs space-y-1">
                            {tier.benefits.map((benefit, i) => (
                              <li key={i} className="flex items-center text-gray-600">
                                <span className="w-1 h-1 bg-blue-600 rounded-full mr-2"></span>
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
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Quantity
                    </label>
                    <div className="flex items-center space-x-3">
                      <button
                        onClick={() => setQuantity(q => Math.max(1, q - 1))}
                        className="w-10 h-10 border rounded-lg hover:bg-gray-100 transition-colors"
                        disabled={quantity <= 1}
                      >
                        -
                      </button>
                      <span className="w-16 text-center font-semibold">{quantity}</span>
                      <button
                        onClick={() => setQuantity(q => Math.min(selectedTier.maxPerUser, q + 1))}
                        className="w-10 h-10 border rounded-lg hover:bg-gray-100 transition-colors"
                        disabled={quantity >= selectedTier.maxPerUser}
                      >
                        +
                      </button>
                    </div>
                  </div>
                )}

                {/* Price Summary */}
                {selectedTier && (
                  <div className="border-t pt-4 mb-6">
                    <div className="flex justify-between mb-2">
                      <span>Price per ticket</span>
                      <span>${selectedTier.price}</span>
                    </div>
                    <div className="flex justify-between mb-2">
                      <span>Quantity</span>
                      <span>{quantity}</span>
                    </div>
                    <div className="flex justify-between font-bold text-lg">
                      <span>Total</span>
                      <span className="text-blue-600">
                        ${(selectedTier.price * quantity).toFixed(2)}
                      </span>
                    </div>
                  </div>
                )}

                {/* Action Buttons */}
                <button
                  onClick={handleBookNow}
                  disabled={!selectedTier}
                  className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition-colors mb-3 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Book Now
                </button>
                
                <button className="w-full border-2 border-blue-600 text-blue-600 py-3 rounded-lg hover:bg-blue-50 transition-colors flex items-center justify-center space-x-2">
                  <Heart className="w-4 h-4" />
                  <span>Save to Wishlist</span>
                </button>
              </>
            )}

            {bookingStep === 'payment' && (
              <div className="space-y-4">
                <h3 className="font-semibold text-lg">Payment Details</h3>
                <p className="text-sm text-gray-600">
                  Booking ID: {booking?.id}
                </p>
                
                {/* Simplified payment form */}
                <div className="space-y-3">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Card Number
                    </label>
                    <input
                      type="text"
                      placeholder="4242 4242 4242 4242"
                      className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Expiry
                      </label>
                      <input type="text" placeholder="MM/YY" className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        CVV
                      </label>
                      <input type="text" placeholder="123" className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" />
                    </div>
                  </div>
                </div>

                <button
                  onClick={handlePayment}
                  disabled={loading}
                  className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 flex items-center justify-center"
                >
                  {loading ? (
                    <>
                      <Loader className="w-4 h-4 animate-spin mr-2" />
                      Processing...
                    </>
                  ) : (
                    `Pay $${booking?.totalAmount?.toFixed(2) || '0.00'}`
                  )}
                </button>
              </div>
            )}

            {bookingStep === 'confirmation' && (
              <div className="text-center py-6">
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <Ticket className="w-8 h-8 text-green-600" />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">Booking Confirmed!</h3>
                <p className="text-gray-600 mb-4">
                  Your tickets have been sent to your email.
                </p>
                <button
                  onClick={() => navigate('/tickets')}
                  className="w-full bg-blue-600 text-white py-3 rounded-lg hover:bg-blue-700 transition-colors"
                >
                  View My Tickets
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default EventDetailPage;