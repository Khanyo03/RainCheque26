import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, LogIn, AlertCircle } from 'lucide-react';
import api from '../services/api';

const LoginPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: 'user@example.com',
    password: 'RainCheque123!'
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

    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify({
      email: data.email,
      fullName: data.fullName,
      role: data.role
    }));

    navigate("/dashboard");
  } catch (err) {
    setError(err.response?.data?.error || "Login failed");
  } finally {
    setLoading(false);
  }
};


  return (
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="border-2 border-black bg-white rounded-2xl shadow-xl max-w-md w-full p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full mb-4">
  <svg 
    width="45" 
    height="45" 
    viewBox="0 0 100 100" 
    fill="none" 
    xmlns="http://www.w3.org/2000/svg"
  >
    {/* Bold Red R */}
    <text x="20" y="68" textAnchor="start" fill="#FF0000" fontSize="64" fontWeight="bold" fontFamily="Arial, sans-serif">
      R
    </text>
    
    {/* CHEQUE text vertical - each letter stacked, reading from bottom to top */}
    <text x="68" y="68" textAnchor="start" fill="#000000" fontSize="6" fontWeight="bold" fontFamily="Arial, sans-serif">
      <tspan x="68" dy="0">C</tspan>
      <tspan x="68" dy="6">H</tspan>
      <tspan x="68" dy="6">E</tspan>
      <tspan x="68" dy="6">Q</tspan>
      <tspan x="68" dy="6">U</tspan>
      <tspan x="68" dy="6">E</tspan>
    </text>
  </svg>
</div>
          <h1 className="text-2xl font-bold text-gray-900">Welcome Back</h1>
          <p className="text-gray-600 mt-2">Sign in to your Rain Cheque account</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border-l-4 border-red-500 p-4 rounded">
            <div className="flex items-center">
              <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
              <p className="text-sm text-red-900">{error}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Email Address
            </label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="w-full pl-10 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                required
              />
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="w-full pl-10 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                required
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full bg-black text-white py-3 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-600">
          Don't have an account?{' '}
          <Link to="/register" className="text-red-600 hover:text-red-700 font-medium">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;