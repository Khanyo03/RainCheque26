import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// Mock react-router-dom
const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
  useNavigate: () => mockNavigate,
  Link: ({ children, to, className }) => (
    <a href={to} className={className}>{children}</a>
  ),
}));

// Mock the api service module with inline mocks
vi.mock('../services/api', () => ({
  default: {},
  adminEventService: {
    list: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
  },
}));

import EventManagementPage from './EventManagementPage';
import { adminEventService } from '../services/api';

const mockEventsResponse = {
  data: {
    content: [
      {
        id: 1,
        title: 'Summer Music Festival',
        category: 'Music',
        status: 'PUBLISHED',
        startTime: '2025-07-15T18:00:00',
      },
      {
        id: 2,
        title: 'Art Exhibition',
        category: 'Art',
        status: 'DRAFT',
        startTime: '2025-08-01T10:00:00',
      },
      {
        id: 3,
        title: 'Cancelled Show',
        category: 'Theatre',
        status: 'CANCELLED',
        startTime: '2025-06-20T20:00:00',
      },
    ],
    totalPages: 2,
    totalElements: 13,
    number: 0,
    size: 10,
  },
};

const emptyResponse = {
  data: {
    content: [],
    totalPages: 0,
    totalElements: 0,
    number: 0,
    size: 10,
  },
};

describe('EventManagementPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    adminEventService.list.mockResolvedValue(mockEventsResponse);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the page header and create button', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getByText('Event Management')).toBeInTheDocument();
    });

    const createLink = screen.getByText('Create New Event');
    expect(createLink.closest('a')).toHaveAttribute('href', '/admin/events/new');
  });

  it('displays events after loading', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.getAllByText('Art Exhibition').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Cancelled Show').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Music').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Art').length).toBeGreaterThanOrEqual(1);
  });

  it('shows loading state initially', () => {
    adminEventService.list.mockReturnValue(new Promise(() => {}));
    render(<EventManagementPage />);

    expect(document.querySelector('.animate-spin')).toBeTruthy();
  });

  it('shows empty state when no events found', async () => {
    adminEventService.list.mockResolvedValue(emptyResponse);

    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getByText('No Events Found')).toBeInTheDocument();
    });

    expect(screen.getByText('Create Your First Event')).toBeInTheDocument();
  });

  it('shows error state when API call fails', async () => {
    adminEventService.list.mockRejectedValue(new Error('Network Error'));

    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getByText('Failed to load events. Please try again.')).toBeInTheDocument();
    });
  });

  it('displays status badges with correct text', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const publishedBadges = screen.getAllByText('PUBLISHED');
    expect(publishedBadges.length).toBeGreaterThanOrEqual(1);

    const draftBadges = screen.getAllByText('DRAFT');
    expect(draftBadges.length).toBeGreaterThanOrEqual(1);

    const cancelledBadges = screen.getAllByText('CANCELLED');
    expect(cancelledBadges.length).toBeGreaterThanOrEqual(1);
  });

  it('navigates to edit page when edit button is clicked', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const editButtons = screen.getAllByLabelText('Edit Summer Music Festival');
    fireEvent.click(editButtons[0]);

    expect(mockNavigate).toHaveBeenCalledWith('/admin/events/1/edit');
  });

  it('shows delete confirmation dialog when delete button is clicked', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const deleteButtons = screen.getAllByLabelText('Delete Summer Music Festival');
    fireEvent.click(deleteButtons[0]);

    expect(screen.getByText('Confirm Delete')).toBeInTheDocument();
    expect(screen.getByText(/Are you sure you want to delete this event/)).toBeInTheDocument();
  });

  it('calls delete API and refreshes when confirmed', async () => {
    adminEventService.delete.mockResolvedValue({});

    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const deleteButtons = screen.getAllByLabelText('Delete Summer Music Festival');
    fireEvent.click(deleteButtons[0]);

    const confirmButton = screen.getByRole('button', { name: 'Delete' });
    fireEvent.click(confirmButton);

    await waitFor(() => {
      expect(adminEventService.delete).toHaveBeenCalledWith(1);
    });
  });

  it('cancels delete when cancel button is clicked', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const deleteButtons = screen.getAllByLabelText('Delete Summer Music Festival');
    fireEvent.click(deleteButtons[0]);

    expect(screen.getByText('Confirm Delete')).toBeInTheDocument();

    const cancelButton = screen.getByRole('button', { name: 'Cancel' });
    fireEvent.click(cancelButton);

    expect(screen.queryByText('Confirm Delete')).not.toBeInTheDocument();
  });

  it('calls updateStatus API when status dropdown is changed', async () => {
    adminEventService.updateStatus.mockResolvedValue({});

    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    const statusDropdowns = screen.getAllByLabelText('Change event status');
    fireEvent.change(statusDropdowns[0], { target: { value: 'CANCELLED' } });

    await waitFor(() => {
      expect(adminEventService.updateStatus).toHaveBeenCalledWith(1, 'CANCELLED');
    });
  });

  it('renders pagination controls when there are multiple pages', async () => {
    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.getByLabelText('Pagination')).toBeInTheDocument();
    expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
  });

  it('does not render pagination when there is only one page', async () => {
    adminEventService.list.mockResolvedValue({
      data: {
        ...mockEventsResponse.data,
        totalPages: 1,
      },
    });

    render(<EventManagementPage />);

    await waitFor(() => {
      expect(screen.getAllByText('Summer Music Festival').length).toBeGreaterThanOrEqual(1);
    });

    expect(screen.queryByLabelText('Pagination')).not.toBeInTheDocument();
  });
});
