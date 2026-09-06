import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Mail, Lock, LogIn, AlertCircle } from 'lucide-react';
import Logo from '../components/Logo';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

const LoginPage = () => {
  const { login } = useAuth();
  const [formData, setFormData] = useState({
    email: '',
    password: ''
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
  setLoading(true);
  setError("");

  try {
    const { data } = await api.post("/auth/login", formData);

    login(data.token, {
      id: data.id,
      email: data.email,
      fullName: data.fullName,
      role: data.role
    });
  } catch (err) {
    setError(err.response?.data?.error || "Login failed");
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
          Members Only
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
              Welcome Back
            </h1>
            <p className="mt-3 text-[11px] font-bold uppercase tracking-widest text-gray-300">
              Sign in to your Rain Cheque account
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

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="login-email" className="mx-label">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="login-email"
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
              <label htmlFor="login-password" className="mx-label">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" />
                <input
                  id="login-password"
                  type="password"
                  name="password"
                  value={formData.password}
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
              <LogIn className="w-5 h-5" aria-hidden="true" />
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <p className="mt-8 border-t-4 border-black pt-6 text-center text-[11px] font-bold uppercase tracking-widest text-gray-600">
            Don't have an account?{' '}
            <Link
              to="/register"
              className="font-display uppercase text-red-700 underline decoration-4 underline-offset-4 hover:text-black"
            >
              Sign up
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
