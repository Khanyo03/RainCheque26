import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Save, Loader, ArrowLeft } from 'lucide-react';
import { adminEventService } from '../services/api';
import ImageUploader from '../components/ImageUploader';
import LoadingSpinner from '../components/LoadingSpinner';

function EventFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const isEditMode = Boolean(id);

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    shortDescription: '',
    venueId: '',
    startTime: '',
    endTime: '',
    category: '',
    featured: false,
    metadata: '',
  });

  const [bannerFile, setBannerFile] = useState(null);
  const [thumbnailFile, setThumbnailFile] = useState(null);
  const [currentBannerUrl, setCurrentBannerUrl] = useState(null);
  const [currentThumbnailUrl, setCurrentThumbnailUrl] = useState(null);

  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  const [loading, setLoading] = useState(false);
  const [pageLoading, setPageLoading] = useState(isEditMode);

  useEffect(() => {
    if (isEditMode) {
      fetchEvent();
    }
  }, [id]);

  const fetchEvent = async () => {
    try {
      setPageLoading(true);
      const response = await adminEventService.get(id);
      const event = response.data;

      setFormData({
        title: event.title || '',
        description: event.description || '',
        shortDescription: event.shortDescription || '',
        venueId: event.venueId || '',
        startTime: formatDateTimeLocal(event.startTime),
        endTime: formatDateTimeLocal(event.endTime),
        category: event.category || '',
        featured: event.featured || false,
        metadata: event.metadata ? JSON.stringify(event.metadata, null, 2) : '',
      });

      setCurrentBannerUrl(event.bannerImageUrl || null);
      setCurrentThumbnailUrl(event.thumbnailImageUrl || null);
    } catch (err) {
      if (err.response?.status === 404) {
        setGeneralError('Event not found.');
      } else {
        setGeneralError('Failed to load event. Please try again.');
      }
    } finally {
      setPageLoading(false);
    }
  };

  const formatDateTimeLocal = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    // Format as YYYY-MM-DDTHH:mm for datetime-local input
    const pad = (n) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  };

  const validate = () => {
    const errors = {};

    if (!formData.title.trim()) {
      errors.title = 'Title is required.';
    } else if (formData.title.trim().length < 3 || formData.title.trim().length > 200) {
      errors.title = 'Title must be between 3 and 200 characters.';
    }

    if (formData.description && formData.description.length > 10000) {
      errors.description = 'Description must not exceed 10000 characters.';
    }

    if (formData.shortDescription && formData.shortDescription.length > 500) {
      errors.shortDescription = 'Short description must not exceed 500 characters.';
    }

    if (!formData.venueId) {
      errors.venueId = 'Venue is required.';
    }

    if (!formData.startTime) {
      errors.startTime = 'Start time is required.';
    }

    if (!formData.endTime) {
      errors.endTime = 'End time is required.';
    }

    if (formData.startTime && formData.endTime) {
      const start = new Date(formData.startTime);
      const end = new Date(formData.endTime);
      if (end <= start) {
        errors.endTime = 'End time must be after start time.';
      }
    }

    if (!formData.category.trim()) {
      errors.category = 'Category is required.';
    }

    if (formData.metadata.trim()) {
      try {
        JSON.parse(formData.metadata);
      } catch {
        errors.metadata = 'Metadata must be valid JSON.';
      }
    }

    return errors;
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
    // Clear field error when user starts editing
    if (fieldErrors[name]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGeneralError('');
    setFieldErrors({});

    const errors = validate();
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    const payload = {
      title: formData.title.trim(),
      description: formData.description || null,
      shortDescription: formData.shortDescription || null,
      venueId: Number(formData.venueId),
      startTime: formData.startTime,
      endTime: formData.endTime,
      category: formData.category.trim(),
      featured: formData.featured,
      metadata: formData.metadata.trim() ? JSON.parse(formData.metadata) : null,
    };

    try {
      setLoading(true);
      let savedEvent;

      if (isEditMode) {
        const response = await adminEventService.update(id, payload);
        savedEvent = response.data;
      } else {
        const response = await adminEventService.create(payload);
        savedEvent = response.data;
      }

      // Upload images after event is saved
      const eventId = savedEvent.id || id;
      if (bannerFile) {
        await adminEventService.uploadImage(eventId, bannerFile, 'banner');
      }
      if (thumbnailFile) {
        await adminEventService.uploadImage(eventId, thumbnailFile, 'thumbnail');
      }

      navigate('/admin/events');
    } catch (err) {
      if (err.response?.data?.fieldErrors) {
        const serverErrors = {};
        err.response.data.fieldErrors.forEach(({ field, message }) => {
          serverErrors[field] = message;
        });
        setFieldErrors(serverErrors);
      } else if (err.response?.data?.message) {
        setGeneralError(err.response.data.message);
      } else {
        setGeneralError('An unexpected error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (pageLoading) {
    return <LoadingSpinner fullPage />;
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <div className="mb-6">
        <button
          type="button"
          onClick={() => navigate('/admin/events')}
          className="flex items-center text-gray-600 hover:text-blue-600 mb-4 transition-colors"
        >
          <ArrowLeft className="w-4 h-4 mr-1" />
          Back to Events
        </button>
        <h1 className="text-2xl font-bold text-gray-900">
          {isEditMode ? 'Edit Event' : 'Create Event'}
        </h1>
      </div>

      {/* General Error */}
      {generalError && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700" role="alert">
          {generalError}
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {/* Title */}
        <div>
          <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
            Title <span className="text-red-500">*</span>
          </label>
          <input
            id="title"
            name="title"
            type="text"
            value={formData.title}
            onChange={handleChange}
            maxLength={200}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              fieldErrors.title ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.title}
            aria-describedby={fieldErrors.title ? 'title-error' : undefined}
          />
          {fieldErrors.title && (
            <p id="title-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.title}
            </p>
          )}
        </div>

        {/* Description */}
        <div>
          <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleChange}
            rows={5}
            maxLength={10000}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-y ${
              fieldErrors.description ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.description}
            aria-describedby={fieldErrors.description ? 'description-error' : undefined}
          />
          {fieldErrors.description && (
            <p id="description-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.description}
            </p>
          )}
        </div>

        {/* Short Description */}
        <div>
          <label htmlFor="shortDescription" className="block text-sm font-medium text-gray-700 mb-1">
            Short Description
          </label>
          <textarea
            id="shortDescription"
            name="shortDescription"
            value={formData.shortDescription}
            onChange={handleChange}
            rows={2}
            maxLength={500}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-y ${
              fieldErrors.shortDescription ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.shortDescription}
            aria-describedby={fieldErrors.shortDescription ? 'shortDescription-error' : undefined}
          />
          {fieldErrors.shortDescription && (
            <p id="shortDescription-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.shortDescription}
            </p>
          )}
        </div>

        {/* Venue ID */}
        <div>
          <label htmlFor="venueId" className="block text-sm font-medium text-gray-700 mb-1">
            Venue ID <span className="text-red-500">*</span>
          </label>
          <input
            id="venueId"
            name="venueId"
            type="number"
            value={formData.venueId}
            onChange={handleChange}
            min={1}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              fieldErrors.venueId ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.venueId}
            aria-describedby={fieldErrors.venueId ? 'venueId-error' : undefined}
          />
          {fieldErrors.venueId && (
            <p id="venueId-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.venueId}
            </p>
          )}
        </div>

        {/* Start Time & End Time - side by side on desktop, stacked on mobile */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label htmlFor="startTime" className="block text-sm font-medium text-gray-700 mb-1">
              Start Time <span className="text-red-500">*</span>
            </label>
            <input
              id="startTime"
              name="startTime"
              type="datetime-local"
              value={formData.startTime}
              onChange={handleChange}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                fieldErrors.startTime ? 'border-red-500' : 'border-gray-300'
              }`}
              aria-invalid={!!fieldErrors.startTime}
              aria-describedby={fieldErrors.startTime ? 'startTime-error' : undefined}
            />
            {fieldErrors.startTime && (
              <p id="startTime-error" className="mt-1 text-sm text-red-600" role="alert">
                {fieldErrors.startTime}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="endTime" className="block text-sm font-medium text-gray-700 mb-1">
              End Time <span className="text-red-500">*</span>
            </label>
            <input
              id="endTime"
              name="endTime"
              type="datetime-local"
              value={formData.endTime}
              onChange={handleChange}
              className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                fieldErrors.endTime ? 'border-red-500' : 'border-gray-300'
              }`}
              aria-invalid={!!fieldErrors.endTime}
              aria-describedby={fieldErrors.endTime ? 'endTime-error' : undefined}
            />
            {fieldErrors.endTime && (
              <p id="endTime-error" className="mt-1 text-sm text-red-600" role="alert">
                {fieldErrors.endTime}
              </p>
            )}
          </div>
        </div>

        {/* Category */}
        <div>
          <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
            Category <span className="text-red-500">*</span>
          </label>
          <input
            id="category"
            name="category"
            type="text"
            value={formData.category}
            onChange={handleChange}
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              fieldErrors.category ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.category}
            aria-describedby={fieldErrors.category ? 'category-error' : undefined}
          />
          {fieldErrors.category && (
            <p id="category-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.category}
            </p>
          )}
        </div>

        {/* Featured */}
        <div className="flex items-center gap-2">
          <input
            id="featured"
            name="featured"
            type="checkbox"
            checked={formData.featured}
            onChange={handleChange}
            className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
          />
          <label htmlFor="featured" className="text-sm font-medium text-gray-700">
            Featured Event
          </label>
        </div>

        {/* Metadata (JSON) */}
        <div>
          <label htmlFor="metadata" className="block text-sm font-medium text-gray-700 mb-1">
            Metadata (JSON)
          </label>
          <textarea
            id="metadata"
            name="metadata"
            value={formData.metadata}
            onChange={handleChange}
            rows={4}
            placeholder='{"weatherPolicy": true, "ageRestriction": "18+"}'
            className={`w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-y font-mono text-sm ${
              fieldErrors.metadata ? 'border-red-500' : 'border-gray-300'
            }`}
            aria-invalid={!!fieldErrors.metadata}
            aria-describedby={fieldErrors.metadata ? 'metadata-error' : undefined}
          />
          {fieldErrors.metadata && (
            <p id="metadata-error" className="mt-1 text-sm text-red-600" role="alert">
              {fieldErrors.metadata}
            </p>
          )}
        </div>

        {/* Image Uploaders */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <ImageUploader
            label="Banner Image"
            currentImageUrl={currentBannerUrl}
            onUpload={(file) => setBannerFile(file)}
          />
          <ImageUploader
            label="Thumbnail Image"
            currentImageUrl={currentThumbnailUrl}
            onUpload={(file) => setThumbnailFile(file)}
          />
        </div>

        {/* Submit Button */}
        <div className="pt-4">
          <button
            type="submit"
            disabled={loading}
            className="w-full md:w-auto px-8 py-3 bg-blue-600 text-white font-medium rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            {loading ? (
              <>
                <Loader className="w-5 h-5 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="w-5 h-5" />
                {isEditMode ? 'Update Event' : 'Create Event'}
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}

export default EventFormPage;
