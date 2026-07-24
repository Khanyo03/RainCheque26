import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, AlertCircle } from 'lucide-react';
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
    <div className="min-h-[80vh] flex items-center justify-center">
      <div className="border-2 border-black bg-white rounded-2xl shadow-xl max-w-md w-full p-8">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-full mb-4">
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    version="1.0" 
    width="45" 
    height="45" 
    viewBox="200 200 400 400"  // Adjusted viewBox to focus on the actual content
    preserveAspectRatio="xMidYMid meet"
  >
    <g transform="translate(-200, 870) scale(0.1, -0.1)" fill="#000000" stroke="none">
      <path d="M2515 3120 c0 -619 1 -872 2 -563 2 310 2 816 0 1125 -1 310 -2 57 -2 -562z"/>
      <path d="M2570 4167 c0 -1 -1 -431 -3 -955 l-2 -952 193 0 192 0 0 435 0 435 30 0 c51 0 98 -25 107 -57 4 -15 8 -205 8 -420 l0 -393 188 0 187 0 0 398 c0 435 -4 469 -57 533 -15 17 -44 39 -65 49 l-37 16 43 22 c56 27 93 75 118 154 17 52 20 89 20 218 0 232 -29 353 -100 424 -84 85 -150 97 -544 95 -153 0 -278 -1 -278 -2z m675 -43 c22 -4 42 -8 45 -9 59 -21 99 -55 100 -83 0 -7 4 -10 10 -7 5 3 7 0 4 -8 -3 -8 0 -18 5 -22 19 -11 39 -71 34 -101 -3 -21 -2 -25 5 -15 13 19 19 -57 17 -234 -2 -126 -4 -153 -18 -205 -15 -57 -57 -114 -98 -136 -45 -23 -68 -45 -62 -60 3 -8 9 -12 13 -9 5 3 15 -1 23 -9 8 -8 17 -12 21 -9 8 8 76 -68 70 -78 -2 -4 0 -10 5 -13 5 -3 12 -23 16 -43 10 -49 6 -775 -4 -786 -4 -4 -73 -6 -153 -5 l-146 3 -7 375 c-9 441 -8 420 -19 413 -4 -3 -5 5 -1 17 5 15 4 20 -4 16 -6 -4 -11 -3 -11 2 0 13 -46 32 -78 32 -18 0 -23 3 -14 9 8 5 0 7 -22 7 -33 -2 -78 -26 -48 -26 7 0 12 5 12 10 0 6 5 10 12 10 6 0 3 -8 -7 -20 -10 -11 -20 -18 -24 -16 -3 2 -4 -182 -1 -410 4 -320 2 -413 -7 -410 -7 3 -10 0 -7 -7 3 -9 -33 -11 -148 -9 -84 2 -155 6 -158 9 -3 2 -5 415 -5 916 l0 912 5 -905 5 -905 3 910 2 910 298 -2 c163 -1 315 -5 337 -9z"/>
      <path d="M2937 3824 c3 -8 0 -14 -6 -14 -6 0 -11 5 -11 12 0 6 -3 9 -6 5 -3 -3 -2 -12 2 -19 9 -14 6 -277 -3 -292 -4 -5 0 -6 7 -1 9 6 12 4 8 -7 -4 -8 1 -29 9 -46 15 -30 18 -32 72 -32 44 0 62 5 79 20 12 12 22 25 22 31 0 6 5 7 10 4 6 -3 10 0 11 7 0 7 3 22 6 33 10 37 11 217 1 245 -18 52 -42 65 -128 66 -63 2 -77 -1 -73 -12z m165 -32 c15 -15 24 -191 13 -249 -11 -58 -44 -83 -110 -83 l-55 0 0 178 0 177 70 -5 c38 -3 75 -11 82 -18z"/>
      <path d="M3366 3352 c-3 -5 1 -9 9 -9 8 0 12 4 9 9 -3 4 -7 8 -9 8 -2 0 -6 -4 -9 -8z"/>
      <path d="M3364 3139 c-8 -14 14 -19 25 -6 8 10 7 14 -5 14 -8 0 -17 -4 -20 -8z"/>
      <path d="M3500 4011 c0 -6 4 -13 10 -16 6 -3 7 1 4 9 -7 18 -14 21 -14 7z"/>
      <path d="M3577 3482 c-37 -39 -38 -99 -1 -136 37 -36 104 -37 139 -1 36 35 35 102 -1 139 -38 37 -100 37 -137 -2z m123 -12 c21 -21 26 -69 10 -100 -23 -43 -116 -40 -134 5 -23 60 10 115 69 115 22 0 43 -8 55 -20z"/>
      <path d="M3607 3452 c-22 -24 -22 -55 1 -75 24 -22 55 -21 75 1 23 25 21 55 -3 77 -22 20 -27 7 -10 -25 7 -14 6 -23 -6 -34 -20 -21 -31 -20 -43 1 -6 11 -6 24 0 36 15 28 6 41 -14 19z"/>
      <path d="M3533 3327 c4 -10 2 -17 -5 -17 -6 0 -9 -3 -5 -6 3 -3 12 -2 20 3 13 7 13 10 -1 23 -14 13 -15 12 -9 -3z"/>
      <path d="M3410 3270 c-12 -7 -10 -9 7 -7 12 0 19 5 17 9 -6 10 -6 10 -24 -2z"/>
      <path d="M3636 3248 c-24 -34 -20 -75 10 -104 24 -24 30 -26 109 -23 97 5 125 21 125 76 0 42 -30 73 -70 73 -27 0 -30 -3 -30 -29 0 -24 5 -30 25 -33 41 -5 33 -23 -11 -26 l-42 -3 5 45 6 46 -56 0 c-46 0 -59 -4 -71 -22z m94 -53 c0 -16 -55 -22 -64 -6 -8 13 5 21 36 21 18 0 28 -5 28 -15z"/>
      <path d="M3412 3234 c10 -10 20 -16 22 -13 3 3 -5 11 -17 18 -21 13 -21 12 -5 -5z"/>
      <path d="M3620 3070 l0 -30 105 0 c87 0 105 -3 105 -15 0 -12 -18 -15 -105 -15 l-105 0 0 -30 0 -30 103 0 c56 0 112 3 125 6 26 8 39 48 22 69 -9 10 -9 15 -1 15 6 0 11 14 11 30 l0 30 -130 0 -130 0 0 -30z"/>
      <path d="M3985 2929 c-4 -6 -5 -12 -2 -15 2 -3 7 2 10 11 7 17 1 20 -8 4z"/>
      <path d="M3620 2900 c0 -17 5 -30 12 -30 9 0 9 -3 0 -12 -18 -18 -14 -56 7 -68 28 -14 214 -13 229 2 16 16 15 50 0 67 -11 11 -6 13 25 8 37 -6 37 -5 37 29 l0 34 -155 0 -155 0 0 -30z m240 -10 c0 -13 -11 -13 -30 0 -12 8 -11 10 8 10 12 0 22 -4 22 -10z m-30 -35 c0 -12 -16 -15 -80 -15 -73 0 -91 6 -73 23 3 4 39 7 80 7 58 0 73 -3 73 -15z"/>
      <path d="M3982 2870 c0 -14 2 -19 5 -12 2 6 2 18 0 25 -3 6 -5 1 -5 -13z"/>
      <path d="M3646 2739 c-33 -26 -36 -79 -6 -109 17 -17 33 -20 110 -20 104 0 130 15 130 73 0 40 -23 64 -68 73 -30 6 -32 5 -32 -24 0 -25 4 -31 25 -34 41 -5 34 -23 -10 -26 l-40 -3 3 46 4 45 -45 0 c-30 0 -53 -7 -71 -21z m84 -54 c0 -16 -55 -22 -64 -6 -8 13 5 21 36 21 18 0 28 -5 28 -15z m20 -35 c0 -5 -5 -10 -11 -10 -5 0 -7 5 -4 10 3 6 8 10 11 10 2 0 4 -4 4 -10z"/>
      <path d="M3903 2735 c0 -8 4 -12 9 -9 5 3 6 10 3 15 -9 13 -12 11 -12 -6z"/>
      <path d="M3650 2577 c-32 -16 -37 -48 -10 -72 24 -22 25 -31 4 -39 -14 -5 -15 -4 -5 8 13 15 -4 26 -40 26 -15 0 -19 -7 -19 -32 l0 -33 150 0 150 0 0 32 0 33 -106 0 c-80 0 -105 3 -101 13 3 8 33 12 105 12 l102 0 0 32 0 32 -102 0 c-63 0 -113 -5 -128 -12z"/>
      <path d="M3625 2412 c-29 -6 -45 -31 -45 -73 0 -58 23 -69 150 -69 98 0 108 2 128 23 48 51 16 116 -61 124 -47 6 -47 6 -47 -25 0 -28 3 -31 38 -34 73 -6 31 -23 -64 -26 -75 -2 -95 0 -92 10 3 7 19 14 36 16 28 3 32 7 32 32 0 25 -4 29 -27 27 -16 0 -37 -3 -48 -5z"/>
      <path d="M3873 2255 c0 -8 4 -12 9 -9 5 3 6 10 3 15 -9 13 -12 11 -12 -6z"/>
      <path d="M3566 2252 c-3 -5 1 -9 9 -9 8 0 12 4 9 9 -3 4 -7 8 -9 8 -2 0 -6 -4 -9 -8z"/>
    </g>
  </svg>
</div>
          <h1 className="text-2xl font-bold text-gray-900">Create Account</h1>
          <p className="text-gray-600 mt-2">Join Rain Cheque today</p>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border-l-4 border-red-500 p-4 rounded">
            <div className="flex items-center">
              <AlertCircle className="w-5 h-5 text-red-500 mr-2" />
              <p className="text-sm text-red-700">{error}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Full Name
            </label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                className="w-full pl-10 px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-black"
                required
              />
            </div>
          </div>

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

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Confirm Password
            </label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
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
            {loading ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-gray-600">
          Already have an account?{' '}
          <Link to="/login" className="text-red-600 hover:text-black font-medium">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
};

export default RegisterPage;