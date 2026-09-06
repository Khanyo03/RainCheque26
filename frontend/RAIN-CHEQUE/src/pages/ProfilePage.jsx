import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Phone, Lock, Save } from 'lucide-react';

const ProfilePage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [validationErrors, setValidationErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    const raw = localStorage.getItem('user');
    if (!raw || raw.trim() === '') {
      navigate('/login');
      return;
    }
    try {
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        navigate('/login');
        return;
      }
      setFormData((prev) => ({
        ...prev,
        fullName: parsed.fullName || '',
        email: parsed.email || '',
        phone: ''
      }));
    } catch {
      navigate('/login');
      return;
    }
  }, [navigate]);

  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => {
        setSuccessMessage('');
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    // Clear validation error for the field being edited
    if (validationErrors[e.target.name]) {
      setValidationErrors((prev) => {
        const updated = { ...prev };
        delete updated[e.target.name];
        return updated;
      });
    }
  };

  const validate = () => {
    const errors = {};
    if (!formData.fullName || formData.fullName.trim().length === 0) {
      errors.fullName = 'Full name is required.';
    } else if (formData.fullName.length > 100) {
      errors.fullName = 'Full name must be 100 characters or fewer.';
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!formData.email || !emailRegex.test(formData.email)) {
      errors.email = 'Please enter a valid email address.';
    }

    return errors;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      return;
    }
    setValidationErrors({});

    // Update localStorage, preserving existing fields like role
    const existingUser = JSON.parse(localStorage.getItem('user'));
    const updated = {
      ...existingUser,
      fullName: formData.fullName,
      email: formData.email,
      phone: formData.phone
    };
    localStorage.setItem('user', JSON.stringify(updated));

    setSuccessMessage('Profile updated successfully.');
  };

  return (
    <div className="max-w-3xl mx-auto">
      {/* Page Heading */}
      <div className="relative overflow-hidden border-8 border-black bg-black p-8 shadow-brutal-lg md:p-10">
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 bg-mx-dots-red mx-dots-sm opacity-60"
        />
        <h1 className="relative font-display uppercase tracking-tighter text-3xl md:text-5xl text-white">
          Profile Settings
        </h1>
      </div>
      <div aria-hidden="true" className="mb-8 h-4 border-x-8 border-b-8 border-black bg-red-700" />

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {successMessage && (
          <div className="border-4 border-black bg-black px-5 py-4 text-white shadow-brutal">
            <p className="font-display uppercase tracking-widest text-[11px] text-red-600">
              Success
            </p>
            <p className="mt-1 text-sm font-bold uppercase tracking-wide">{successMessage}</p>
          </div>
        )}

        {/* Personal Information */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="flex items-center gap-3 border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            <User className="w-5 h-5 text-white" aria-hidden="true" />
            Personal Information
          </h2>

          <div className="space-y-5 p-6">
            <div>
              <label className="mx-label">
                Full Name
              </label>
              <input
                type="text"
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                className="mx-input"
              />
              {validationErrors.fullName && (
                <p className="mt-2 font-display uppercase text-xs tracking-widest text-red-700">{validationErrors.fullName}</p>
              )}
            </div>

            <div>
              <label className="mx-label">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" aria-hidden="true" />
                <input
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="mx-input pl-12"
                />
              </div>
              {validationErrors.email && (
                <p className="mt-2 font-display uppercase text-xs tracking-widest text-red-700">{validationErrors.email}</p>
              )}
            </div>

            <div>
              <label className="mx-label">
                Phone Number
              </label>
              <div className="relative">
                <Phone className="absolute left-4 top-1/2 transform -translate-y-1/2 text-red-700 w-5 h-5" aria-hidden="true" />
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleChange}
                  className="mx-input pl-12"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Change Password */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="flex items-center gap-3 border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            <Lock className="w-5 h-5 text-white" aria-hidden="true" />
            Change Password
          </h2>

          <div className="space-y-5 p-6">
            <div>
              <label className="mx-label">
                Current Password
              </label>
              <input
                type="password"
                name="currentPassword"
                value={formData.currentPassword}
                onChange={handleChange}
                className="mx-input"
              />
            </div>

            <div>
              <label className="mx-label">
                New Password
              </label>
              <input
                type="password"
                name="newPassword"
                value={formData.newPassword}
                onChange={handleChange}
                className="mx-input"
              />
            </div>

            <div>
              <label className="mx-label">
                Confirm New Password
              </label>
              <input
                type="password"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className="mx-input"
              />
            </div>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end">
          <button
            type="submit"
            className="inline-flex items-center gap-2 border-4 border-black bg-red-700 px-8 py-3 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 active:translate-x-[3px] active:translate-y-[3px] active:shadow-none"
          >
            <Save className="w-5 h-5" aria-hidden="true" />
            Save Changes
          </button>
        </div>
      </form>
    </div>
  );
};

export default ProfilePage;
