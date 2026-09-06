import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, AlertCircle } from 'lucide-react';
import Logo from '../components/Logo';
import api from '../services/api';

const RegisterPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const response = await api.post('/auth/register', {
        fullName: formData.fullName,
        email: formData.email,
        password: formData.password
      });
      
      // Store user data
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify({
        email: response.data.email,
        fullName: response.data.fullName,
        role: response.data.role
      }));
      
      // Redirect to dashboard instead of home
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-4 py-12">
      <div className="relative w-full max-w-md border-8 border-black bg-white shadow-brutal-lg">
        {/* Decorative accent badge */}
        <span
          aria-hidden="true"
          className="mx-sticker absolute -top-6 right-6 z-10 text-xs"
        >
          New Here?
        </span>

        {/* Black header band with red dot texture */}
        <div className="relative overflow-hidden border-b-8 border-black bg-black px-6 pt-10 pb-7 text-center">
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 bg-mx-dots-red mx-dots-sm opacity-80"
          />
          <div className="relative">
            <div className="mb-6 flex justify-center">
              <span className="inline-block border-4 border-black bg-white px-5 py-3 shadow-brutal-red">
                <Logo className="h-12 w-auto text-black" />
              </span>
            </div>
            <h1 className="font-display uppercase tracking-tighter text-3xl md:text-4xl text-white">
              Create Account
            </h1>
            <p className="mt-3 text-[11px] font-bold uppercase tracking-widest text-gray-300">
              Join Rain Cheque today
            </p>
          </div>
        </div>

        {/* Chunky rule between header and form */}
        <div aria-hidden="true" className="h-3 bg-red-700" />

        <div className="p-8">
          {error && (
            <div className="mb-6 flex items-start gap-3 border-4 border-black bg-red-700 p-4 text-white shadow-brutal">
              <AlertCircle className="mt-0.5 w-5 h-5 shrink-0 text-white" />
              <div>
                <p className="font-display text-xs uppercase tracking-widest">Error</p>
                <p className="mt-1 text-sm font-bold">{error}</p>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label htmlFor="register-fullName" className="mx-label">
                Full Name
              </label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="register-fullName"
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  className="mx-input pl-12"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="register-email" className="mx-label">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="register-email"
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="mx-input pl-12"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="register-password" className="mx-label">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="register-password"
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  className="mx-input pl-12"
                  required
                />
              </div>
            </div>

            <div>
              <label htmlFor="register-confirmPassword" className="mx-label">
                Confirm Password
              </label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="register-confirmPassword"
                  type="password"
                  name="confirmPassword"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className="mx-input pl-12"
                  required
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full inline-flex items-center justify-center gap-2 border-4 border-black bg-red-700 px-6 py-4 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none disabled:opacity-50 disabled:pointer-events-none"
            >
              {loading ? 'Creating Account...' : 'Sign Up'}
            </button>
          </form>

          <p className="mt-8 border-t-4 border-black pt-6 text-center text-[11px] font-bold uppercase tracking-widest text-gray-600">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-display uppercase text-red-700 underline decoration-4 underline-offset-4 hover:text-black"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
