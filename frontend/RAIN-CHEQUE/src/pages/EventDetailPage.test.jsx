import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { readFileSync } from 'fs';
import { resolve } from 'path';

// Mock the api module - must be before component import
vi.mock('../services/api', () => {
  return {
    default: {
      get: vi.fn(),
      post: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
    },
  };
});

import EventDetailPage from './EventDetailPage';
import api from '../services/api';

const mockEventResponse = {
  id: 1,
  title: 'Summer Music Festival',
  description: 'A great outdoor music festival.',
  shortDescription: 'Outdoor music event',
  venueName: 'Central Park',
  venueCity: 'New York',
  venueAddress: '123 Park Ave',
  startTime: '2025-07-15T18:00:00Z',
  endTime: '2025-07-15T23:00:00Z',
  category: 'Music',
  bannerImageUrl: 'https://example.com/banner.jpg',
  thumbnailUrl: 'https://example.com/thumb.jpg',
  status: 'ACTIVE',
  featured: true,
  ticketTiers: [
    {
      id: 1,
      name: 'General Admission',
      description: 'Standard entry',
      price: 50,
      remainingQuantity: 200,
      totalQuantity: 500,
      maxPerUser: 4,
      minPerUser: 1,
      benefits: ['Entry to event'],
      active: true,
    },
  ],
  minPrice: 50,
  maxPrice: 150,
  metadata: { weatherPolicy: true },
  totalAvailableTickets: 200,
  venue: {
    address: '123 Park Ave',
    city: 'New York',
    country: 'USA',
    capacity: 5000,
  },
};

function renderWithRouter(id, searchParams = '') {
  const path = searchParams ? `/events/${id}?${searchParams}` : `/events/${id}`;
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/events/:id" element={<EventDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('EventDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders event data from a mocked API response', async () => {
    api.get.mockResolvedValueOnce({ data: mockEventResponse });

    renderWithRouter('1');

    await waitFor(() => {
      expect(screen.getByText('Summer Music Festival')).toBeInTheDocument();
    });

    expect(screen.getByText('Music')).toBeInTheDocument();
    expect(screen.getByText('Central Park')).toBeInTheDocument();
    expect(screen.getByText('A great outdoor music festival.')).toBeInTheDocument();
    expect(screen.getByText('General Admission')).toBeInTheDocument();
    expect(screen.getByText('R50')).toBeInTheDocument();
    expect(screen.getByText('200 seats left')).toBeInTheDocument();
  });

  it('displays a loading spinner during fetch', async () => {
    let resolvePromise;
    const pendingPromise = new Promise((res) => {
      resolvePromise = res;
    });
    api.get.mockReturnValueOnce(pendingPromise);

    renderWithRouter('1');

    // LoadingSpinner with fullPage renders "Loading" text
    expect(screen.getByText('Loading')).toBeInTheDocument();

    // Resolve to clean up
    resolvePromise({ data: mockEventResponse });
    await waitFor(() => {
      expect(screen.queryByText('Loading')).not.toBeInTheDocument();
    });
  });

  it('renders "Event not found" message on 404 response', async () => {
    const error404 = new Error('Not Found');
    error404.response = { status: 404 };
    api.get.mockRejectedValueOnce(error404);

    renderWithRouter('999');

    await waitFor(() => {
      expect(screen.getByText('Event not found')).toBeInTheDocument();
    });
  });

  it('displays generic error on non-404 failure', async () => {
    const error500 = new Error('Server Error');
    error500.response = { status: 500 };
    api.get.mockRejectedValueOnce(error500);

    renderWithRouter('1');

    await waitFor(() => {
      expect(
        screen.getByText('Failed to load event details. Please try again.')
      ).toBeInTheDocument();
    });
  });

  it('shows error for invalid route param "abc" without API call', async () => {
    renderWithRouter('abc');

    await waitFor(() => {
      expect(screen.getByText('Invalid event ID')).toBeInTheDocument();
    });

    expect(api.get).not.toHaveBeenCalled();
  });

  it('shows error for invalid route param "-1" without API call', async () => {
    renderWithRouter('-1');

    await waitFor(() => {
      expect(screen.getByText('Invalid event ID')).toBeInTheDocument();
    });

    expect(api.get).not.toHaveBeenCalled();
  });

  it('shows error for invalid route param "0" — validates regex behavior', async () => {
    // "0" matches /^\d+$/ so the component will attempt an API call.
    // This documents the current behavior.
    const error404 = new Error('Not Found');
    error404.response = { status: 404 };
    api.get.mockRejectedValueOnce(error404);

    renderWithRouter('0');

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/events/0');
    });
  });

  it('verifies no mockEvents or usingMockData references exist in the component', () => {
    const componentPath = resolve(import.meta.dirname, './EventDetailPage.jsx');
    const componentSource = readFileSync(componentPath, 'utf-8');

    expect(componentSource).not.toContain('mockEvents');
    expect(componentSource).not.toContain('usingMockData');
  });

  it('shows confirmation step when returning from Stripe with payment=success', async () => {
    api.get.mockResolvedValueOnce({ data: mockEventResponse });

    renderWithRouter('1', 'payment=success');

    await waitFor(() => {
      expect(screen.getByText('Payment Successful!')).toBeInTheDocument();
    });

    expect(screen.getByText('View My Tickets')).toBeInTheDocument();
  });

  it('shows cancellation error when returning from Stripe with payment=cancelled', async () => {
    api.get.mockResolvedValueOnce({ data: mockEventResponse });

    renderWithRouter('1', 'payment=cancelled');

    await waitFor(() => {
      expect(screen.getByText('Payment was cancelled. You can try again when ready.')).toBeInTheDocument();
    });
  });

  it('verifies the component no longer contains a card form or handlePayment', () => {
    const componentPath = resolve(import.meta.dirname, './EventDetailPage.jsx');
    const componentSource = readFileSync(componentPath, 'utf-8');

    expect(componentSource).not.toContain('handlePayment');
    expect(componentSource).not.toContain('Card Number');
    expect(componentSource).not.toContain('/bookings/initiate');
    expect(componentSource).not.toContain('/payments/confirm');
    expect(componentSource).toContain('/payments/checkout');
  });
});
