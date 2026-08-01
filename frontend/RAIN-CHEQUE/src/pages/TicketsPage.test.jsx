import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import TicketsPage from './TicketsPage';

// Mock react-router-dom
vi.mock('react-router-dom', () => ({
  useNavigate: () => vi.fn(),
  Link: ({ children, to }) => <a href={to}>{children}</a>,
}));

// Mock the api module
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

// Import the mocked api after mocking
import api from '../services/api';

// Mock URL methods
const mockCreateObjectURL = vi.fn(() => 'blob:mock-url');
const mockRevokeObjectURL = vi.fn();
global.URL.createObjectURL = mockCreateObjectURL;
global.URL.revokeObjectURL = mockRevokeObjectURL;

const mockTicketsResponse = [
  {
    id: 1,
    ticketCode: 'TCK-12345678',
    status: 'ACTIVE',
    pricePaid: 49.99,
    seatNumber: 'A12',
    qrSecret: 'secret-qr-123',
    booking: {
      id: 100,
      event: {
        title: 'Summer Music Festival',
        startTime: '2025-07-15T18:00:00',
        venue: { name: 'Main Arena' },
      },
    },
    ticketTier: { name: 'VIP' },
  },
  {
    id: 2,
    ticketCode: 'TCK-87654321',
    status: 'USED',
    pricePaid: 29.99,
    seatNumber: null,
    qrSecret: 'secret-qr-456',
    booking: {
      id: 100,
      event: {
        title: 'Summer Music Festival',
        startTime: '2025-07-15T18:00:00',
        venue: { name: 'Main Arena' },
      },
    },
    ticketTier: { name: 'General' },
  },
];

describe('TicketsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders tickets from a mocked API response', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    // Both tickets share the same event, so use getAllByText
    expect(screen.getAllByText('Summer Music Festival')).toHaveLength(2);
    expect(screen.getByText('VIP')).toBeInTheDocument();
    expect(screen.getByText('General')).toBeInTheDocument();
    expect(screen.getByText('Download All (2)')).toBeInTheDocument();
  });

  it('renders empty state when API returns an empty array', async () => {
    api.get.mockResolvedValueOnce({ data: [] });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('No Tickets Yet')).toBeInTheDocument();
    });

    expect(screen.getByText(/When you purchase tickets/)).toBeInTheDocument();
    expect(screen.getByText('Browse Events')).toBeInTheDocument();
    expect(screen.getByText('Browse Events').closest('a')).toHaveAttribute('href', '/events');
  });

  it('renders error state when API call fails', async () => {
    api.get.mockRejectedValueOnce(new Error('Network Error'));

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('Failed to load tickets. Please try again.')).toBeInTheDocument();
    });
  });

  it('clicking download triggers the correct API call with blob responseType', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    // Mock the download response
    const pdfBlob = new Blob(['pdf content'], { type: 'application/pdf' });
    api.get.mockResolvedValueOnce({
      data: pdfBlob,
      headers: {
        'content-type': 'application/pdf',
        'content-disposition': 'attachment; filename="ticket-1.pdf"',
      },
    });

    // Mock DOM link creation for file download
    const mockLink = { href: '', setAttribute: vi.fn(), click: vi.fn(), remove: vi.fn() };
    const originalCreateElement = document.createElement.bind(document);
    vi.spyOn(document, 'createElement').mockImplementation((tag) => {
      if (tag === 'a') return mockLink;
      return originalCreateElement(tag);
    });
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => {});

    // Click the first download button
    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/tickets/1/download', {
        responseType: 'blob',
        timeout: 30000,
      });
    });
  });

  it('shows "Ticket not found" error on 404 download failure', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    api.get.mockRejectedValueOnce({
      response: { status: 404 },
      code: undefined,
    });

    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Ticket not found.')).toBeInTheDocument();
    });
  });

  it('shows "not authorized" error on 401 download failure', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    api.get.mockRejectedValueOnce({
      response: { status: 401 },
      code: undefined,
    });

    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('You are not authorized to download this ticket.')).toBeInTheDocument();
    });
  });

  it('shows "Access denied" error on 403 download failure', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    api.get.mockRejectedValueOnce({
      response: { status: 403 },
      code: undefined,
    });

    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Access denied.')).toBeInTheDocument();
    });
  });

  it('shows timeout error when download times out', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    api.get.mockRejectedValueOnce({
      code: 'ECONNABORTED',
      response: undefined,
    });

    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Request timed out. Please try again.')).toBeInTheDocument();
    });
  });

  it('shows server error with status code for other download failures', async () => {
    api.get.mockResolvedValueOnce({ data: mockTicketsResponse });

    render(<TicketsPage />);

    await waitFor(() => {
      expect(screen.getByText('My Tickets')).toBeInTheDocument();
    });

    api.get.mockRejectedValueOnce({
      response: { status: 500 },
      code: undefined,
    });

    const downloadButtons = screen.getAllByText('Download Ticket');
    fireEvent.click(downloadButtons[0]);

    await waitFor(() => {
      expect(screen.getByText('Server error: 500')).toBeInTheDocument();
    });
  });

  it('does not contain mockTickets array in the component source', async () => {
    const fs = await import('fs');
    const path = await import('path');
    const componentPath = path.resolve(import.meta.dirname, './TicketsPage.jsx');
    const source = fs.readFileSync(componentPath, 'utf8');
    expect(source).not.toMatch(/mockTickets\s*=/);
  });
});
