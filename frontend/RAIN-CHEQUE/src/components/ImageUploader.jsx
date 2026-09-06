import React, { useState, useRef, useCallback } from 'react';
import { Upload, X, AlertCircle } from 'lucide-react';

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

function ImageUploader({ onUpload, label = 'Upload Image', currentImageUrl = null }) {
  const [preview, setPreview] = useState(currentImageUrl);
  const [error, setError] = useState('');
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef(null);

  const validateFile = (file) => {
    if (!ALLOWED_TYPES.includes(file.type)) {
      return 'Invalid file type. Allowed: JPEG, PNG, WebP.';
    }
    if (file.size > MAX_SIZE_BYTES) {
      return 'File too large. Maximum size is 5 MB.';
    }
    return null;
  };

  const handleFile = useCallback((file) => {
    setError('');

    const validationError = validateFile(file);
    if (validationError) {
      setError(validationError);
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      setPreview(e.target.result);
    };
    reader.readAsDataURL(file);

    if (onUpload) {
      onUpload(file);
    }
  }, [onUpload]);

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFile(file);
    }
  };

  const handleInputChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleFile(file);
    }
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  const clearPreview = () => {
    setPreview(null);
    setError('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return (
    <div className="w-full">
      {label && (
        <label className="mx-label">
          {label}
        </label>
      )}

      <div
        role="button"
        tabIndex={0}
        aria-label={`${label}. Drag and drop or click to browse. Accepted formats: JPEG, PNG, WebP. Max size: 5 MB.`}
        className={`relative border-4 border-dashed p-6 text-center cursor-pointer transition-colors
          ${isDragging ? 'border-black bg-red-700 text-white' : 'border-black bg-white hover:bg-gray-100'}
          ${error ? 'border-red-700 bg-primary-50' : ''}
        `}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={handleClick}
        onKeyDown={handleKeyDown}
      >
        {preview ? (
          <div className="relative">
            <img
              src={preview}
              alt="Upload preview"
              className="mx-auto max-h-48 border-4 border-black object-contain shadow-brutal-sm"
            />
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                clearPreview();
              }}
              className="absolute top-1 right-1 border-4 border-black bg-white p-1 text-black shadow-brutal-sm transition-colors hover:bg-red-700 hover:text-white"
              aria-label="Remove image"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3">
            <span
              aria-hidden="true"
              className={`flex h-16 w-16 items-center justify-center border-4 border-black shadow-brutal-sm ${
                isDragging ? 'bg-black' : 'bg-white'
              }`}
            >
              <Upload className={`w-8 h-8 ${isDragging ? 'text-white' : 'text-red-700'}`} />
            </span>
            <p
              className={`font-display uppercase tracking-widest text-xs ${
                isDragging ? 'text-white' : 'text-black'
              }`}
            >
              Drag and drop an image, or <span className={`underline decoration-4 underline-offset-2 ${isDragging ? 'text-white' : 'text-red-700'}`}>click to browse</span>
            </p>
            <p
              className={`text-[10px] font-bold uppercase tracking-widest ${
                isDragging ? 'text-white' : 'text-gray-500'
              }`}
            >
              JPEG, PNG, or WebP. Max 5 MB.
            </p>
          </div>
        )}

        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={handleInputChange}
          className="hidden"
          aria-hidden="true"
        />
      </div>

      {error && (
        <div className="flex items-center gap-2 mt-2 font-display uppercase text-xs tracking-widest text-red-700" role="alert">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}
    </div>
  );
}

export default ImageUploader;
