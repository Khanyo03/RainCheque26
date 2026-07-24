import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Menu, X, Ticket, User, LogOut, LayoutDashboard } from 'lucide-react';

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

  return (
    <nav className="bg-black shadow-lg sticky top-0 z-50">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <Link to="/" className="flex items-center space-x-2">
            <Ticket className="w-8 h-8 text-red-600" />
            <span className="text-xl font-bold text-white">RAIN CHEQUE</span>
          </Link>

          <div className="hidden md:flex items-center space-x-6">
            
            
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className="text-white hover:text-red-600 flex items-center">
                  <LayoutDashboard className="w-5 h-5 mr-1" />
                  Dashboard
                </Link>
                <Link to="/events" className="text-white hover:text-red-600 flex items-center">
                   <LayoutDashboard className="w-5 h-5 mr-1" />
                  Events
                </Link>
                <Link to="/tickets" className="text-white hover:text-red-600 flex items-center ">
                  <Ticket classname="w-5 h-5 mr-1" />
                  Tickets
               </Link>
                <div className="relative group">
                  <button className="flex items-center space-x-1 text-white hover:text-red-600">
                    <User className="w-5 h-5" />
                    <span>{user?.fullName || 'Profile'}</span>
                  </button>
                  <div className="absolute right-0 mt-2 w-48 bg-black rounded-lg shadow-lg py-2 hidden group-hover:block">
                    <Link to="/profile" className="block px-4 py-2 text-white hover:bg-gray-100">
                      Profile Settings
                    </Link>
                    <button
                      onClick={handleLogout}
                      className="block w-full text-left px-4 py-2 text-red-600 hover:bg-gray-100"
                    >
                      <LogOut className="w-4 h-4 inline mr-2" />
                      Logout
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-3">
                <Link to="/login" className="border-2 border-red-600 text-blue-600 px-4 py-2 rounded-lg hover:bg-blue-50">
                  Login
                </Link>
                <Link to="/register" className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
                  Sign Up
                </Link>
              </div>
            )}
          </div>

          <button onClick={() => setIsOpen(!isOpen)} className="md:hidden">
            {isOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {isOpen && (
          <div className="md:hidden py-4 border-t">
            <Link to="/events" className="block py-2" onClick={() => setIsOpen(false)}>Events</Link>
            
            {isLoggedIn ? (
              <>
                <Link to="/dashboard" className="block py-2" onClick={() => setIsOpen(false)}>Dashboard</Link>
                <Link to="/tickets" className="block py-2" onClick={() => setIsOpen(false)}>My Tickets</Link>
                <Link to="/profile" className="block py-2" onClick={() => setIsOpen(false)}>Profile</Link>
                <button onClick={handleLogout} className="block w-full text-left py-2 text-red-600">
                  Logout
                </button>
              </>
            ) : (
              <div className="space-y-2 pt-2">
                <Link to="/login" className="block border-2 border-red-600 text-red-600 px-4 py-2 rounded-lg text-center">
                  Login
                </Link>
                <Link to="/register" className="block bg-red-600 text-white px-4 py-2 rounded-lg text-center">
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