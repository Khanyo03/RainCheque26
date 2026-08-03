import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import EventFormPage from './EventFormPage';

// Mock the api service
vi.mock('../services/api', () => ({
  adminEventService: {
    get: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    uploadImage: vi.fn(),
  },
}));

// Mock the ImageUploader component
vi.mock('../components/ImageUploader', () => ({
  default: ({ label, onUpload, currentImageUrl }) => (
    <div data-testid={`image-uploader-${label?.replace(/\s+/g, '-').toLowerCase()}`}>
      <span>{label}</span>
      <button onClick={() => onUpload && onUpload(new File(['test'], 'test.png', { type: 'image/png' }))}>
        Upload
      </button>
      {currentImageUrl && <img src={currentImageUrl} alt="preview" />}
    </div>
  ),
}));

// Mock the LoadingSpinner component
vi.mock('../components/LoadingSpinner', () => ({
  default: ({ fullPage }) => <div data-testid="loading-spinner">{fullPage ? 'Full Page Loading' : 'Loading'}</div>,
}));

// Mock react-router-dom hooks
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

import { adminEventService } from '../services/api';

function renderCreateMode() {
  return render(
    <MemoryRouter initialEntries={['/admin/events/new']}>
      <Routes>
        <Route path="/admin/events/new" element={<EventFormPage />} />
      </Routes>
    </MemoryRouter>
  );
}

function renderEditMode(id = '1') {
  return render(
    <MemoryRouter initialEntries={[`/admin/events/${id}/edit`]}>
      <Routes>
        <Route path="/admin/events/:id/edit" element={<EventFormPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('EventFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Create mode', () => {
    it('renders form with all required fields', () => {
      renderCreateMode();

      expect(screen.getByLabelText(/^title/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^description$/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/short description/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/venue id/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/start time/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/end time/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^category/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/featured event/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/metadata/i)).toBeInTheDocument();
    });

    it('renders the Create Event heading', () => {
      renderCreateMode();
      expect(screen.getByRole('heading', { name: /create event/i })).toBeInTheDocument();
    });

    it('renders image uploaders for banner and thumbnail', () => {
      renderCreateMode();
      expect(screen.getByTestId('image-uploader-banner-image')).toBeInTheDocument();
      expect(screen.getByTestId('image-uploader-thumbnail-image')).toBeInTheDocument();
    });

    it('shows inline validation errors for empty required fields on submit', async () => {
      renderCreateMode();

      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/title is required/i)).toBeInTheDocument();
        expect(screen.getByText(/venue is required/i)).toBeInTheDocument();
        expect(screen.getByText(/start time is required/i)).toBeInTheDocument();
        expect(screen.getByText(/end time is required/i)).toBeInTheDocument();
        expect(screen.getByText(/category is required/i)).toBeInTheDocument();
      });
    });

    it('shows validation error for title shorter than 3 chars', async () => {
      renderCreateMode();

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'ab' } });
      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/title must be between 3 and 200 characters/i)).toBeInTheDocument();
      });
    });

    it('shows validation error when end time is before start time', async () => {
      renderCreateMode();

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Test Event' } });
      fireEvent.change(screen.getByLabelText(/venue id/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/start time/i), { target: { value: '2025-12-01T10:00' } });
      fireEvent.change(screen.getByLabelText(/end time/i), { target: { value: '2025-12-01T09:00' } });
      fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Music' } });

      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/end time must be after start time/i)).toBeInTheDocument();
      });
    });

    it('disables submit button and shows loading spinner during save', async () => {
      adminEventService.create.mockReturnValue(new Promise(() => {})); // never resolves

      renderCreateMode();

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Test Event' } });
      fireEvent.change(screen.getByLabelText(/venue id/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/start time/i), { target: { value: '2025-12-01T10:00' } });
      fireEvent.change(screen.getByLabelText(/end time/i), { target: { value: '2025-12-01T11:00' } });
      fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Music' } });

      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/saving/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /saving/i })).toBeDisabled();
      });
    });

    it('calls adminEventService.create and navigates on success', async () => {
      adminEventService.create.mockResolvedValue({ data: { id: 42 } });

      renderCreateMode();

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Test Event' } });
      fireEvent.change(screen.getByLabelText(/venue id/i), { target: { value: '1' } });
      fireEvent.change(screen.getByLabelText(/start time/i), { target: { value: '2025-12-01T10:00' } });
      fireEvent.change(screen.getByLabelText(/end time/i), { target: { value: '2025-12-01T11:00' } });
      fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Music' } });

      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(adminEventService.create).toHaveBeenCalledWith(expect.objectContaining({
          title: 'Test Event',
          venueId: 1,
          category: 'Music',
        }));
        expect(mockNavigate).toHaveBeenCalledWith('/admin/events');
      });
    });

    it('displays server-side field errors', async () => {
      adminEventService.create.mockRejectedValue({
        response: {
          data: {
            fieldErrors: [
              { field: 'title', message: 'Title already exists' },
              { field: 'venueId', message: 'Venue not found' },
            ],
          },
        },
      });

      renderCreateMode();

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Test Event' } });
      fireEvent.change(screen.getByLabelText(/venue id/i), { target: { value: '999' } });
      fireEvent.change(screen.getByLabelText(/start time/i), { target: { value: '2025-12-01T10:00' } });
      fireEvent.change(screen.getByLabelText(/end time/i), { target: { value: '2025-12-01T11:00' } });
      fireEvent.change(screen.getByLabelText(/category/i), { target: { value: 'Music' } });

      const submitButton = screen.getByRole('button', { name: /create event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText('Title already exists')).toBeInTheDocument();
        expect(screen.getByText('Venue not found')).toBeInTheDocument();
      });
    });
  });

  describe('Edit mode', () => {
    const mockEvent = {
      id: 1,
      title: 'Existing Event',
      description: 'A great event',
      shortDescription: 'Great event',
      venueId: 5,
      startTime: '2025-12-15T18:00:00',
      endTime: '2025-12-15T22:00:00',
      category: 'Concert',
      featured: true,
      metadata: { weatherPolicy: true },
      bannerImageUrl: '/uploads/banner.jpg',
      thumbnailImageUrl: '/uploads/thumb.jpg',
    };

    it('shows loading spinner while fetching event', () => {
      adminEventService.get.mockReturnValue(new Promise(() => {}));
      renderEditMode('1');
      expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
    });

    it('pre-fills form fields when editing', async () => {
      adminEventService.get.mockResolvedValue({ data: mockEvent });
      renderEditMode('1');

      await waitFor(() => {
        expect(screen.getByLabelText(/title/i)).toHaveValue('Existing Event');
        expect(screen.getByLabelText(/category/i)).toHaveValue('Concert');
        expect(screen.getByLabelText(/featured event/i)).toBeChecked();
      });
    });

    it('renders the Edit Event heading', async () => {
      adminEventService.get.mockResolvedValue({ data: mockEvent });
      renderEditMode('1');

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: /edit event/i })).toBeInTheDocument();
      });
    });

    it('calls adminEventService.update on submit in edit mode', async () => {
      adminEventService.get.mockResolvedValue({ data: mockEvent });
      adminEventService.update.mockResolvedValue({ data: { ...mockEvent, title: 'Updated Event' } });

      renderEditMode('1');

      await waitFor(() => {
        expect(screen.getByLabelText(/title/i)).toHaveValue('Existing Event');
      });

      fireEvent.change(screen.getByLabelText(/title/i), { target: { value: 'Updated Event' } });
      const submitButton = screen.getByRole('button', { name: /update event/i });
      fireEvent.click(submitButton);

      await waitFor(() => {
        expect(adminEventService.update).toHaveBeenCalledWith('1', expect.objectContaining({
          title: 'Updated Event',
        }));
        expect(mockNavigate).toHaveBeenCalledWith('/admin/events');
      });
    });
  });
});
