import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import DashboardPage from './DashboardPage';

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock the API service
vi.mock('../services/api', () => ({
  default: {
    get: vi.fn(),
  },
}));

import api from '../services/api';

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const renderDashboard = () => {
    return render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );
  };

  describe('User from localStorage', () => {
    it('reads user from localStorage and displays fullName in welcome header', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', memberSince: 'January 2024' };
      localStorage.setItem('user', JSON.stringify(user));
      api.get.mockResolvedValue({ data: [] });

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Welcome back, Jane Smith!/)).toBeInTheDocument();
      });
    });

    it('displays memberSince when present in user data', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', memberSince: 'January 2024' };
      localStorage.setItem('user', JSON.stringify(user));
      api.get.mockResolvedValue({ data: [] });

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText(/Member since January 2024/)).toBeInTheDocument();
      });
    });
  });

  describe('Ticket count from API', () => {
    it('fetches ticket count from API and displays it', async () => {
      const user = { fullName: 'John Doe', email: 'john@example.com' };
      localStorage.setItem('user', JSON.stringify(user));
      api.get.mockResolvedValue({ data: [{ id: 1 }, { id: 2 }, { id: 3 }] });

      renderDashboard();

      await waitFor(() => {
        expect(api.get).toHaveBeenCalledWith('/tickets');
      });

      await waitFor(() => {
        expect(screen.getByText('3')).toBeInTheDocument();
      });
      expect(screen.getByText('Tickets Purchased')).toBeInTheDocument();
    });

    it('shows loading spinner while fetching tickets', async () => {
      const user = { fullName: 'John Doe', email: 'john@example.com' };
      localStorage.setItem('user', JSON.stringify(user));
      // Never-resolving promise to keep loading state
      api.get.mockReturnValue(new Promise(() => {}));

      renderDashboard();

      // LoadingSpinner renders an animate-spin element
      await waitFor(() => {
        expect(screen.getByText(/Welcome back, John Doe!/)).toBeInTheDocument();
      });
      expect(document.querySelector('.animate-spin')).toBeInTheDocument();
    });
  });

  describe('Redirect to /login when no user', () => {
    it('redirects to /login when localStorage has no user key', () => {
      renderDashboard();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is empty string', () => {
      localStorage.setItem('user', '');

      renderDashboard();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is whitespace only', () => {
      localStorage.setItem('user', '   ');

      renderDashboard();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  describe('Redirect to /login when invalid JSON', () => {
    it('redirects to /login when localStorage has invalid JSON', () => {
      localStorage.setItem('user', 'not-valid-json{');

      renderDashboard();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is null JSON value', () => {
      localStorage.setItem('user', 'null');

      renderDashboard();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  describe('Error state', () => {
    it('shows 0 for ticket count when API call fails', async () => {
      const user = { fullName: 'John Doe', email: 'john@example.com' };
      localStorage.setItem('user', JSON.stringify(user));
      api.get.mockRejectedValue(new Error('Network error'));

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('0')).toBeInTheDocument();
      });
      expect(screen.getByText('Tickets Purchased')).toBeInTheDocument();
    });

    it('displays error alert when API call fails', async () => {
      const user = { fullName: 'John Doe', email: 'john@example.com' };
      localStorage.setItem('user', JSON.stringify(user));
      api.get.mockRejectedValue(new Error('Network error'));

      renderDashboard();

      await waitFor(() => {
        expect(screen.getByText('Failed to load ticket data.')).toBeInTheDocument();
      });
    });
  });

  describe('No hardcoded user object', () => {
    it('does not render any user data when localStorage is empty', () => {
      renderDashboard();

      // Should not display any welcome message with a hardcoded name
      expect(screen.queryByText(/Welcome back/)).not.toBeInTheDocument();
    });

    it('renders null (nothing) when user is not set', () => {
      const { container } = renderDashboard();

      // Component returns null before user is set, container should be empty
      expect(container.querySelector('.bg-gradient-to-r')).not.toBeInTheDocument();
    });
  });
});
