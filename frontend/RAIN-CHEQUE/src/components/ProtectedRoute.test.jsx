import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

let mockIsAuthenticated = false;
const mockSetRedirectPath = vi.fn();

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    get isAuthenticated() { return mockIsAuthenticated; },
    setRedirectPath: mockSetRedirectPath,
  }),
}));

import ProtectedRoute from './ProtectedRoute';

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = false;
  });

  afterEach(() => {
    mockIsAuthenticated = false;
  });

  const renderProtectedRoute = (initialRoute = '/dashboard') => {
    return render(
      <MemoryRouter initialEntries={[initialRoute]}>
        <Routes>
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <div data-testid="protected-content">Dashboard Content</div>
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <div data-testid="protected-content">Profile Content</div>
              </ProtectedRoute>
            }
          />
          <Route path="/login" element={<div data-testid="login-page">Login Page</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  describe('unauthenticated users', () => {
    it('redirects to /login when user is not authenticated', () => {
      mockIsAuthenticated = false;

      renderProtectedRoute('/dashboard');

      expect(screen.getByTestId('login-page')).toBeInTheDocument();
      expect(screen.queryByTestId('protected-content')).not.toBeInTheDocument();
    });

    it('calls setRedirectPath with the current pathname', () => {
      mockIsAuthenticated = false;

      renderProtectedRoute('/dashboard');

      expect(mockSetRedirectPath).toHaveBeenCalledWith('/dashboard');
    });

    it('saves the correct path when navigating to /profile', () => {
      mockIsAuthenticated = false;

      renderProtectedRoute('/profile');

      expect(mockSetRedirectPath).toHaveBeenCalledWith('/profile');
      expect(screen.getByTestId('login-page')).toBeInTheDocument();
    });
  });

  describe('authenticated users', () => {
    it('renders children when user is authenticated', () => {
      mockIsAuthenticated = true;

      renderProtectedRoute('/dashboard');

      expect(screen.getByTestId('protected-content')).toBeInTheDocument();
      expect(screen.getByText('Dashboard Content')).toBeInTheDocument();
    });

    it('does not redirect authenticated users', () => {
      mockIsAuthenticated = true;

      renderProtectedRoute('/dashboard');

      expect(screen.queryByTestId('login-page')).not.toBeInTheDocument();
    });

    it('does not call setRedirectPath when authenticated', () => {
      mockIsAuthenticated = true;

      renderProtectedRoute('/dashboard');

      expect(mockSetRedirectPath).not.toHaveBeenCalled();
    });
  });
});
