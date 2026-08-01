import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';

// Mock the api module's setupInterceptors before importing AuthContext
vi.mock('../services/api', () => ({
  default: {
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
  setupInterceptors: vi.fn(),
}));

import { AuthProvider, useAuth } from './AuthContext';

// Helper component that exposes auth context values for testing
function AuthConsumer({ onAuth }) {
  const auth = useAuth();
  onAuth(auth);
  return (
    <div>
      <span data-testid="is-authenticated">{String(auth.isAuthenticated)}</span>
      <span data-testid="token">{auth.token || ''}</span>
      <span data-testid="user">{auth.user ? JSON.stringify(auth.user) : ''}</span>
      <button data-testid="login-btn" onClick={() => auth.login('test-token-123', { id: 1, email: 'test@example.com' })}>
        Login
      </button>
      <button data-testid="logout-btn" onClick={() => auth.logout()}>
        Logout
      </button>
    </div>
  );
}

describe('AuthContext', () => {
  let authValues;

  const renderWithAuth = (initialEntries = ['/']) => {
    authValues = null;
    return render(
      <MemoryRouter initialEntries={initialEntries}>
        <AuthProvider>
          <AuthConsumer onAuth={(auth) => { authValues = auth; }} />
        </AuthProvider>
      </MemoryRouter>
    );
  };

  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('isAuthenticated', () => {
    it('starts as false when no token in localStorage', () => {
      renderWithAuth();

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(authValues.isAuthenticated).toBe(false);
    });

    it('starts as true when token is present in localStorage', () => {
      localStorage.setItem('token', 'existing-token');
      localStorage.setItem('user', JSON.stringify({ id: 1, email: 'user@test.com' }));

      renderWithAuth();

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
      expect(authValues.isAuthenticated).toBe(true);
    });
  });

  describe('login', () => {
    it('sets isAuthenticated to true after login', async () => {
      renderWithAuth();

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');

      await act(async () => {
        await userEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');
    });

    it('persists token to localStorage', async () => {
      renderWithAuth();

      await act(async () => {
        await userEvent.click(screen.getByTestId('login-btn'));
      });

      expect(localStorage.getItem('token')).toBe('test-token-123');
    });

    it('persists user to localStorage', async () => {
      renderWithAuth();

      await act(async () => {
        await userEvent.click(screen.getByTestId('login-btn'));
      });

      const storedUser = JSON.parse(localStorage.getItem('user'));
      expect(storedUser).toEqual({ id: 1, email: 'test@example.com' });
    });

    it('exposes token and user in context after login', async () => {
      renderWithAuth();

      await act(async () => {
        await userEvent.click(screen.getByTestId('login-btn'));
      });

      expect(screen.getByTestId('token').textContent).toBe('test-token-123');
      expect(JSON.parse(screen.getByTestId('user').textContent)).toEqual({ id: 1, email: 'test@example.com' });
    });
  });

  describe('logout', () => {
    it('clears token and user and sets isAuthenticated to false', async () => {
      localStorage.setItem('token', 'existing-token');
      localStorage.setItem('user', JSON.stringify({ id: 1, email: 'user@test.com' }));

      renderWithAuth();

      expect(screen.getByTestId('is-authenticated').textContent).toBe('true');

      await act(async () => {
        await userEvent.click(screen.getByTestId('logout-btn'));
      });

      expect(screen.getByTestId('is-authenticated').textContent).toBe('false');
      expect(screen.getByTestId('token').textContent).toBe('');
      expect(screen.getByTestId('user').textContent).toBe('');
    });

    it('removes token from localStorage', async () => {
      localStorage.setItem('token', 'existing-token');
      localStorage.setItem('user', JSON.stringify({ id: 1, email: 'user@test.com' }));

      renderWithAuth();

      await act(async () => {
        await userEvent.click(screen.getByTestId('logout-btn'));
      });

      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('user')).toBeNull();
    });
  });

  describe('useAuth outside provider', () => {
    it('throws an error when used outside AuthProvider', () => {
      // Suppress React error boundary console output
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      function BadComponent() {
        useAuth();
        return null;
      }

      expect(() => {
        render(
          <MemoryRouter>
            <BadComponent />
          </MemoryRouter>
        );
      }).toThrow('useAuth must be used within an AuthProvider');

      consoleSpy.mockRestore();
    });
  });
});
