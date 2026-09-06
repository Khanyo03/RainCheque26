import React from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Ticket, Shield, Cloud, ArrowRight } from 'lucide-react';

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

  const tickerItems = ['RAIN OR SHINE', 'INSTANT TICKETS', 'NO REFUND DRAMA', 'BOOK WITH CONFIDENCE'];

  return (
    <div className="space-y-12">
      {/* ============ HERO ============ */}
      <section className="relative border-8 border-black bg-black text-white shadow-brutal-red-lg overflow-hidden">
        {/* Layered texture */}
        <div className="absolute inset-0 bg-gradient-to-br from-red-700 via-black to-black" aria-hidden="true"></div>
        <div className="absolute inset-0 bg-mx-stripes opacity-20" aria-hidden="true"></div>
        <div className="absolute -right-10 -top-10 h-56 w-56 bg-mx-dots-white mx-dots-sm opacity-30" aria-hidden="true"></div>

        <div className="relative px-6 py-16 md:px-12 md:py-24">
          <span className="mx-sticker text-sm tracking-widest">Never miss a moment</span>

          <h1 className="mt-6 font-display uppercase leading-[0.85] tracking-tighter">
            <span className="block text-5xl md:text-7xl">Never Miss</span>
            <span className="block text-5xl md:text-7xl">Any Moment</span>
            <span className="mt-2 block text-6xl md:text-8xl text-red-600 mx-outline-white">
              Rain Cheque
            </span>
          </h1>

          <div className="my-8 h-3 w-40 bg-white" aria-hidden="true"></div>

          <p className="max-w-2xl text-lg md:text-xl font-medium text-white">
            Discover amazing events and book with confidence. If weather cancels your event,
            we&apos;ve got you covered with our Rain Cheque guarantee.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row gap-4">
            <Link to="/events" className="mx-btn bg-white text-black border-white shadow-brutal-red">
              Browse Events
              <ArrowRight className="w-5 h-5" />
            </Link>
            <Link to="/how-it-works" className="mx-btn bg-black text-white border-white shadow-brutal-white">
              Learn More
            </Link>
          </div>
        </div>
      </section>

      {/* ============ TICKER ============ */}
      <section
        className="border-y-8 border-black bg-red-700 overflow-hidden -mx-4"
        aria-hidden="true"
      >
        <div className="mx-marquee-track py-3 font-display uppercase tracking-widest text-white text-lg">
          {[...tickerItems, ...tickerItems, ...tickerItems].map((item, i) => (
            <span key={i} className="flex items-center gap-10">
              {item}
              <span className="text-black">★</span>
            </span>
          ))}
        </div>
      </section>

      {/* ============ FEATURES ============ */}
      <section className="border-8 border-black bg-white shadow-brutal-lg">
        <div className="border-b-8 border-black bg-black px-6 py-6 relative overflow-hidden">
          <div className="absolute inset-0 bg-mx-dots-red mx-dots-sm opacity-40" aria-hidden="true"></div>
          <h2 className="relative text-center font-display uppercase tracking-tighter text-3xl md:text-5xl text-white">
            Why Choose Rain Cheque?
          </h2>
        </div>

        <div className="grid gap-0 md:grid-cols-2 lg:grid-cols-4">
          {features.map((feature, index) => (
            <div
              key={index}
              className="group relative border-b-4 border-r-4 border-black p-8 last:border-r-0 hover:bg-red-700 transition-colors duration-150"
            >
              <span className="absolute right-3 top-2 font-display text-5xl text-black/10 group-hover:text-white/30">
                0{index + 1}
              </span>

              <div className="inline-flex h-16 w-16 items-center justify-center border-4 border-black bg-red-700 shadow-brutal-sm group-hover:bg-black">
                {feature.icon}
              </div>

              <h3 className="mt-5 font-display uppercase tracking-tight text-xl text-black group-hover:text-white">
                {feature.title}
              </h3>
              <p className="mt-2 text-sm font-medium text-gray-700 group-hover:text-white">
                {feature.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* ============ CTA ============ */}
      <section className="relative border-8 border-black bg-red-700 text-white shadow-brutal-lg overflow-hidden">
        <div className="absolute inset-0 bg-mx-stripes opacity-15" aria-hidden="true"></div>
        <div className="absolute -left-8 -bottom-8 h-48 w-48 bg-mx-dots-white mx-dots-sm opacity-30" aria-hidden="true"></div>

        <div className="relative px-6 py-14 md:px-12 md:py-20 text-center">
          <h2 className="font-display uppercase tracking-tighter text-3xl md:text-5xl leading-none">
            Ready To Experience
            <span className="block text-black mx-outline-white">Amazing Events?</span>
          </h2>

          <div className="mx-auto my-7 h-3 w-32 bg-black" aria-hidden="true"></div>

          <p className="mx-auto max-w-2xl text-lg md:text-xl font-medium">
            Join thousands of happy customers who book with confidence using Rain Cheque.
          </p>

          <div className="mt-10 flex flex-col sm:flex-row justify-center gap-4">
            <Link to="/register" className="mx-btn bg-black text-white border-black shadow-brutal-white">
              Join us!
            </Link>
            <Link to="/events" className="mx-btn bg-white text-black border-black">
              Browse Events
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
