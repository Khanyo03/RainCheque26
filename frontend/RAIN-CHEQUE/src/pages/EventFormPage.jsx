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
      <div>
        <button
          type="button"
          onClick={() => navigate('/admin/events')}
          className="mb-4 inline-flex items-center gap-2 border-4 border-black bg-white px-3 py-2 font-display text-xs uppercase tracking-widest text-black shadow-brutal-sm transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[2px] active:translate-y-[2px] active:shadow-none"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Events
        </button>
        <div className="relative overflow-hidden border-8 border-black bg-black p-8 shadow-brutal-lg md:p-10">
          <span
            aria-hidden="true"
            className="pointer-events-none absolute inset-0 bg-mx-dots-red mx-dots-sm opacity-60"
          />
          <h1 className="relative font-display uppercase tracking-tighter text-3xl md:text-5xl text-white">
            {isEditMode ? 'Edit Event' : 'Create Event'}
          </h1>
        </div>
        <div aria-hidden="true" className="mb-8 h-4 border-x-8 border-b-8 border-black bg-red-700" />
      </div>

      {/* General Error */}
      {generalError && (
        <div className="mb-6 border-4 border-black bg-red-700 px-5 py-4 text-white shadow-brutal" role="alert">
          <p className="font-display uppercase tracking-widest text-[11px] text-white">
            Error
          </p>
          <p className="mt-1 text-sm font-bold uppercase tracking-wide">{generalError}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} noValidate className="space-y-6">
        {/* Event Details */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            Event Details
          </h2>

          <div className="space-y-5 p-6">
            {/* Title */}
            <div>
              <label htmlFor="title" className="mx-label">
                Title <span className="text-red-700 font-bold">*</span>
              </label>
              <input
                id="title"
                name="title"
                type="text"
                value={formData.title}
                onChange={handleChange}
                maxLength={200}
                className={`mx-input ${fieldErrors.title ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.title}
                aria-describedby={fieldErrors.title ? 'title-error' : undefined}
              />
              {fieldErrors.title && (
                <p id="title-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.title}
                </p>
              )}
            </div>

            {/* Description */}
            <div>
              <label htmlFor="description" className="mx-label">
                Description
              </label>
              <textarea
                id="description"
                name="description"
                value={formData.description}
                onChange={handleChange}
                rows={5}
                maxLength={10000}
                className={`mx-input resize-y ${fieldErrors.description ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.description}
                aria-describedby={fieldErrors.description ? 'description-error' : undefined}
              />
              {fieldErrors.description && (
                <p id="description-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.description}
                </p>
              )}
            </div>

            {/* Short Description */}
            <div>
              <label htmlFor="shortDescription" className="mx-label">
                Short Description
              </label>
              <textarea
                id="shortDescription"
                name="shortDescription"
                value={formData.shortDescription}
                onChange={handleChange}
                rows={2}
                maxLength={500}
                className={`mx-input resize-y ${fieldErrors.shortDescription ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.shortDescription}
                aria-describedby={fieldErrors.shortDescription ? 'shortDescription-error' : undefined}
              />
              {fieldErrors.shortDescription && (
                <p id="shortDescription-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.shortDescription}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Venue & Schedule */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            Venue &amp; Schedule
          </h2>

          <div className="space-y-5 p-6">
            {/* Venue ID */}
            <div>
              <label htmlFor="venueId" className="mx-label">
                Venue ID <span className="text-red-700 font-bold">*</span>
              </label>
              <input
                id="venueId"
                name="venueId"
                type="number"
                value={formData.venueId}
                onChange={handleChange}
                min={1}
                className={`mx-input ${fieldErrors.venueId ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.venueId}
                aria-describedby={fieldErrors.venueId ? 'venueId-error' : undefined}
              />
              {fieldErrors.venueId && (
                <p id="venueId-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.venueId}
                </p>
              )}
            </div>

            {/* Start Time & End Time - side by side on desktop, stacked on mobile */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="startTime" className="mx-label">
                  Start Time <span className="text-red-700 font-bold">*</span>
                </label>
                <input
                  id="startTime"
                  name="startTime"
                  type="datetime-local"
                  value={formData.startTime}
                  onChange={handleChange}
                  className={`mx-input ${fieldErrors.startTime ? 'border-red-700 bg-primary-50' : ''}`}
                  aria-invalid={!!fieldErrors.startTime}
                  aria-describedby={fieldErrors.startTime ? 'startTime-error' : undefined}
                />
                {fieldErrors.startTime && (
                  <p id="startTime-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                    {fieldErrors.startTime}
                  </p>
                )}
              </div>

              <div>
                <label htmlFor="endTime" className="mx-label">
                  End Time <span className="text-red-700 font-bold">*</span>
                </label>
                <input
                  id="endTime"
                  name="endTime"
                  type="datetime-local"
                  value={formData.endTime}
                  onChange={handleChange}
                  className={`mx-input ${fieldErrors.endTime ? 'border-red-700 bg-primary-50' : ''}`}
                  aria-invalid={!!fieldErrors.endTime}
                  aria-describedby={fieldErrors.endTime ? 'endTime-error' : undefined}
                />
                {fieldErrors.endTime && (
                  <p id="endTime-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                    {fieldErrors.endTime}
                  </p>
                )}
              </div>
            </div>
          </div>
        </div>

        {/* Classification */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            Classification
          </h2>

          <div className="space-y-5 p-6">
            {/* Category */}
            <div>
              <label htmlFor="category" className="mx-label">
                Category <span className="text-red-700 font-bold">*</span>
              </label>
              <input
                id="category"
                name="category"
                type="text"
                value={formData.category}
                onChange={handleChange}
                className={`mx-input ${fieldErrors.category ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.category}
                aria-describedby={fieldErrors.category ? 'category-error' : undefined}
              />
              {fieldErrors.category && (
                <p id="category-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.category}
                </p>
              )}
            </div>

            {/* Featured */}
            <div className="flex items-center gap-3 border-4 border-black bg-gray-100 px-4 py-3">
              <input
                id="featured"
                name="featured"
                type="checkbox"
                checked={formData.featured}
                onChange={handleChange}
                className="h-5 w-5 shrink-0 border-4 border-black accent-red-700 focus:outline-none"
              />
              <label htmlFor="featured" className="mx-label mb-0">
                Featured Event
              </label>
            </div>
          </div>
        </div>

        {/* Advanced */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            Advanced
          </h2>

          <div className="space-y-5 p-6">
            {/* Metadata (JSON) */}
            <div>
              <label htmlFor="metadata" className="mx-label">
                Metadata (JSON)
              </label>
              <textarea
                id="metadata"
                name="metadata"
                value={formData.metadata}
                onChange={handleChange}
                rows={4}
                placeholder='{"weatherPolicy": true, "ageRestriction": "18+"}'
                className={`mx-input resize-y font-mono text-sm ${fieldErrors.metadata ? 'border-red-700 bg-primary-50' : ''}`}
                aria-invalid={!!fieldErrors.metadata}
                aria-describedby={fieldErrors.metadata ? 'metadata-error' : undefined}
              />
              {fieldErrors.metadata && (
                <p id="metadata-error" className="mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
                  {fieldErrors.metadata}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Images */}
        <div className="border-4 border-black bg-white shadow-brutal">
          <h2 className="border-b-4 border-black bg-black px-6 py-4 font-display uppercase tracking-tight text-lg text-white">
            Images
          </h2>

          {/* Image Uploaders */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 p-6">
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
        </div>

        {/* Submit Button */}
        <div className="pt-4">
          <button
            type="submit"
            disabled={loading}
            className="inline-flex w-full items-center justify-center gap-2 border-4 border-black bg-red-700 px-8 py-3 font-display uppercase tracking-tight text-white shadow-brutal transition-transform duration-100 hover:-translate-x-[2px] hover:-translate-y-[2px] active:translate-x-[3px] active:translate-y-[3px] active:shadow-none disabled:opacity-50 disabled:pointer-events-none md:w-auto"
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
