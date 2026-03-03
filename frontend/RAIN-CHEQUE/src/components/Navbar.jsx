import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X, Ticket } from 'lucide-react';

const Navbar = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem('token');
    setIsLoggedIn(!!token);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsLoggedIn(false);
    navigate('/');
  };

  return (
    <nav className="bg-white shadow-lg sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <Ticket className="w-8 h-8 text-blue-600" />
            <span className="text-xl font-bold text-gray-800">Rain Cheque</span>
          </Link>

          {/* Desktop Menu */}
          <div className="hidden md:flex items-center space-x-6">
            <Link to="/events" className="text-gray-600 hover:text-blue-600 transition">
              Events
            </Link>
            <Link to="/how-it-works" className="text-gray-600 hover:text-blue-600 transition">
              How It Works
            </Link>
            
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className="text-gray-600 hover:text-blue-600 transition">
                  Dashboard
                </Link>
                <Link to="/tickets" className="text-gray-600 hover:text-blue-600 transition">
                  My Tickets
                </Link>
                <button
                  onClick={handleLogout}
                  className="text-red-600 hover:text-red-700 transition"
                >
                  Logout
                </button>
              </>
            ) : (
              <div className="flex items-center space-x-3">
                <Link 
                  to="/login" 
                  className="border-2 border-blue-600 text-blue-600 px-4 py-2 rounded-lg hover:bg-blue-50 transition"
                >
                  Login
                </Link>
                <Link 
                  to="/register" 
                  className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>

          {/* Mobile menu button */}
          <button 
            onClick={() => setIsOpen(!isOpen)} 
            className="md:hidden text-gray-600 hover:text-gray-900"
          >
            {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Menu */}
        {isOpen && (
          <div className="md:hidden py-4 border-t">
            <Link 
              to="/events" 
              className="block py-2 text-gray-600 hover:text-blue-600"
              onClick={() => setIsOpen(false)}
            >
              Events
            </Link>
            <Link 
              to="/how-it-works" 
              className="block py-2 text-gray-600 hover:text-blue-600"
              onClick={() => setIsOpen(false)}
            >
              How It Works
            </Link>
            
            {isLoggedIn ? (
              <>
                <Link 
                  to="/dashboard" 
                  className="block py-2 text-gray-600 hover:text-blue-600"
                  onClick={() => setIsOpen(false)}
                >
                  Dashboard
                </Link>
                <Link 
                  to="/tickets" 
                  className="block py-2 text-gray-600 hover:text-blue-600"
                  onClick={() => setIsOpen(false)}
                >
                  My Tickets
                </Link>
                <button 
                  onClick={() => {
                    handleLogout();
                    setIsOpen(false);
                  }} 
                  className="block w-full text-left py-2 text-red-600"
                >
                  Logout
                </button>
              </>
            ) : (
              <div className="space-y-2 pt-2">
                <Link 
                  to="/login" 
                  className="block border-2 border-blue-600 text-blue-600 px-4 py-2 rounded-lg text-center"
                  onClick={() => setIsOpen(false)}
                >
                  Login
                </Link>
                <Link 
                  to="/register" 
                  className="block bg-blue-600 text-white px-4 py-2 rounded-lg text-center"
                  onClick={() => setIsOpen(false)}
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        )}
      </div>
    </nav>
  );
};

export default Navbar;