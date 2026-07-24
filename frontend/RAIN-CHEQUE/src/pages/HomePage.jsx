import React from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Ticket, Shield, Cloud } from 'lucide-react';

const HomePage = () => {
  const features = [
    {
      icon: <Shield className="w-8 h-8 text-white" />,
      title: 'Secure Booking',
      description: 'Your transactions are protected with industry-standard security.'
    },
    {
      icon: <Cloud className="w-8 h-8 text-white" />,
      title: 'Rain Cheque Guarantee',
      description: 'Get a full refund or credit if your event is affected by weather.'
    },
    {
      icon: <Calendar className="w-8 h-8 text-white" />,
      title: 'Easy Scheduling',
      description: 'Save events to your calendar and get reminders.'
    },
    {
      icon: <Ticket className="w-8 h-8 text-white" />,
      title: 'Digital Tickets',
      description: 'Access your tickets instantly on your mobile device.'
    }
  ];

  return (
    <div className="space-y-16">
      {/* Hero Section */}
      <section className="border-2 border-black bg-black relative bg-gradient-to-r from-red-700 to-black text-white rounded-3xl overflow-hidden">
        <div className="absolute inset-0 bg-black opacity-20"></div>
        <div className="relative px-8 py-20">
          <div className="max-w-3xl">
            <h1 className="text-4xl md:text-5xl font-bold mb-6">
              Never Miss Any Moment With,
              <span className="block text-black font-bold mb-6">Rain Cheque</span>
            </h1>
            <p className="text-lg md:text-xl mb-8 text-white">
              Discover amazing events and book with confidence. If weather cancels your event, 
              we've got you covered with our Rain Cheque guarantee.
            </p>
            <div className="flex flex-col sm:flex-row space-y-4 sm:space-y-0 sm:space-x-4">
              <Link to="/events" className="bg-black text-white px-8 py-3 rounded-lg font-semibold hover:bg-white hover:text-red-600 transition text-center">
                Browse Events
              </Link>
              <Link to="/how-it-works" className="border-2 border-white text-white px-8 py-3 rounded-lg font-semibold hover:bg-white hover:text-red-600 transition text-center">
                Learn More
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="border-2 border-black bg-black bg-white py-16 rounded-3xl">
        <div className="px-4">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
            Why Choose Rain Cheque?
          </h2>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => (
              <div key={index} className="text-center">
                <div className="inline-flex items-center justify-center w-16 h-16 bg-red-700 rounded-full mb-4">
                  {feature.icon}
                </div>
                <h3 className="text-lg font-semibold text-gray-900 mb-2">{feature.title}</h3>
                <p className="text-gray-600">{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="border-2 border-white bg-black bg-black text-white rounded-3xl p-8 md:p-12 text-center">
        <h2 className="text-2xl md:text-3xl font-bold mb-4">Ready to Experience Amazing Events?</h2>
        <p className="text-lg md:text-xl text-gray-300 mb-8 max-w-2xl mx-auto">
          Join thousands of happy customers who book with confidence using Rain Cheque.
        </p>
        <div className="flex flex-col sm:flex-row justify-center space-y-4 sm:space-y-0 sm:space-x-4">
          <Link to="/register" className="bg-red-700 text-white px-8 py-3 rounded-lg font-semibold hover:bg-white transition hover:text-red-700">
            Join us!
          </Link>
          <Link to="/events" className="border-2 border-white text-white px-8 py-3 rounded-lg font-semibold hover:bg-white hover:text-red-700 transition">
            Browse Events
          </Link>
        </div>
      </section>
    </div>
  );
};

export default HomePage;