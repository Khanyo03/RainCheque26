import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X, Ticket, User, LogOut, LayoutDashboard, Settings } from 'lucide-react';
import Logo from './Logo';

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    checkLoginStatus();
  }, []);

  const checkLoginStatus = () => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    if (token && userData) {
      setIsLoggedIn(true);
      setUser(JSON.parse(userData));
    } else {
      setIsLoggedIn(false);
      setUser(null);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsLoggedIn(false);
    setUser(null);
    navigate('/');
  };

  const navLink =
    'text-white font-display uppercase tracking-tight text-sm flex items-center px-2 py-1 border-2 border-transparent hover:bg-red-700 hover:border-black transition-colors duration-100';

  const mobileLink =
    'block py-3 px-3 font-display uppercase tracking-tight text-white border-b-4 border-red-700 hover:bg-red-700';

  return (
    <nav className="bg-black shadow-lg sticky top-0 z-50 border-b-8 border-red-700">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="flex items-center">
            <Logo className="h-9 w-auto text-white" />
          </Link>

          <div className="hidden md:flex items-center space-x-2">
            
            
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className={navLink}>
                  <LayoutDashboard className="w-5 h-5 mr-1" />
                  Dashboard
                </Link>
                <Link to="/events" className={navLink}>
                   <LayoutDashboard className="w-5 h-5 mr-1" />
                  Events
                </Link>
                <Link to="/tickets" className={navLink}>
                  <Ticket className="w-5 h-5 mr-1" />
                  Tickets
               </Link>
                {(user?.role === 'ADMIN' || user?.role === 'VENUE_ADMIN') && (
                  <Link to="/admin/events" className={navLink}>
                    <Settings className="w-5 h-5 mr-1" />
                    Event Admin
                  </Link>
                )}
                <div className="relative group">
                  <button className="flex items-center space-x-1 text-white font-display uppercase tracking-tight text-sm px-2 py-1 border-2 border-transparent hover:bg-red-700 hover:border-black transition-colors duration-100">
                    <User className="w-5 h-5" />
                    <span>{user?.fullName || 'Profile'}</span>
                  </button>
                  <div className="absolute right-0 mt-2 w-52 border-4 border-white bg-black shadow-brutal-red py-2 hidden group-hover:block">
                    <Link
                      to="/profile"
                      className="block px-4 py-2 font-display uppercase tracking-tight text-xs text-white hover:bg-red-700"
                    >
                      Profile Settings
                    </Link>
                    <div className="my-2 h-1 w-full bg-white" aria-hidden="true"></div>
                    <button
                      onClick={handleLogout}
                      className="block w-full text-left px-4 py-2 font-display uppercase tracking-tight text-xs text-red-600 hover:bg-red-700 hover:text-white"
                    >
                      <LogOut className="w-4 h-4 inline mr-2" />
                      Logout
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-3">
                <Link
                  to="/login"
                  className="inline-flex items-center justify-center border-4 border-white bg-black px-5 py-2 font-display uppercase tracking-tight text-sm text-white shadow-brutal-white-lg transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="inline-flex items-center justify-center border-4 border-white bg-red-700 px-5 py-2 font-display uppercase tracking-tight text-sm text-white shadow-brutal-white-lg transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>

          <button
            onClick={() => setIsOpen(!isOpen)}
            className="md:hidden border-4 border-white bg-red-700 p-1 text-white shadow-brutal-white"
            aria-label={isOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={isOpen}
          >
            {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {isOpen && (
          <div className="md:hidden py-4 border-t-8 border-white">
            <Link to="/events" className={mobileLink} onClick={() => setIsOpen(false)}>Events</Link>
            
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className={mobileLink} onClick={() => setIsOpen(false)}>Dashboard</Link>
                <Link to="/tickets" className={mobileLink} onClick={() => setIsOpen(false)}>My Tickets</Link>
                <Link to="/profile" className={mobileLink} onClick={() => setIsOpen(false)}>Profile</Link>
                {(user?.role === 'ADMIN' || user?.role === 'VENUE_ADMIN') && (
                  <Link to="/admin/events" className={mobileLink} onClick={() => setIsOpen(false)}>Event Admin</Link>
                )}
                <button
                  onClick={handleLogout}
                  className="block w-full text-left py-3 px-3 font-display uppercase tracking-tight text-red-600 hover:bg-red-700 hover:text-white"
                >
                  Logout
                </button>
              </>
            ) : (
              <div className="space-y-3 pt-3">
                <Link
                  to="/login"
                  className="block border-4 border-white bg-black px-4 py-3 text-center font-display uppercase tracking-tight text-white"
                >
                  Login
                </Link>
                <Link
                  to="/register"
                  className="block border-4 border-white bg-red-700 px-4 py-3 text-center font-display uppercase tracking-tight text-white"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Decorative ticker strip */}
      <div
        className="bg-red-700 border-y-4 border-black overflow-hidden"
        aria-hidden="true"
      >
        <div className="mx-marquee-track py-1 font-display uppercase tracking-widest text-[11px] text-white">
          <span>LIVE EVENTS</span>
          <span>★</span>
          <span>INSTANT TICKETS</span>
          <span>★</span>
          <span>RAIN CHEQUE GUARANTEE</span>
          <span>★</span>
          <span>NO REFUND DRAMA</span>
          <span>★</span>
          <span>LIVE EVENTS</span>
          <span>★</span>
          <span>INSTANT TICKETS</span>
          <span>★</span>
          <span>RAIN CHEQUE GUARANTEE</span>
          <span>★</span>
          <span>NO REFUND DRAMA</span>
          <span>★</span>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
