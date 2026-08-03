import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

let mockIsAuthenticated = false;
let mockUser = null;
const mockSetRedirectPath = vi.fn();

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    get isAuthenticated() { return mockIsAuthenticated; },
    get user() { return mockUser; },
    setRedirectPath: mockSetRedirectPath,
  }),
}));

import AdminRoute from './AdminRoute';

describe('AdminRoute', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = false;
    mockUser = null;
  });

  const renderAdminRoute = (initialRoute = '/admin/events') => {
    return render(
      <MemoryRouter initialEntries={[initialRoute]}>
        <Routes>
          <Route
            path="/admin/events"
            element={
              <AdminRoute>
                <div data-testid="admin-content">Admin Content</div>
              </AdminRoute>
            }
          />
          <Route path="/login" element={<div data-testid="login-page">Login Page</div>} />
          <Route path="/dashboard" element={<div data-testid="dashboard-page">Dashboard Page</div>} />
        </Routes>
      </MemoryRouter>
    );
  };

  describe('unauthenticated users', () => {
    it('redirects to /login when user is not authenticated', () => {
      mockIsAuthenticated = false;

      renderAdminRoute();

      expect(screen.getByTestId('login-page')).toBeInTheDocument();
      expect(screen.queryByTestId('admin-content')).not.toBeInTheDocument();
    });

    it('calls setRedirectPath with the current pathname', () => {
      mockIsAuthenticated = false;

      renderAdminRoute('/admin/events');

      expect(mockSetRedirectPath).toHaveBeenCalledWith('/admin/events');
    });
  });

  describe('authenticated users without admin role', () => {
    it('redirects USER role to /dashboard', () => {
      mockIsAuthenticated = true;
      mockUser = { id: 1, email: 'user@test.com', fullName: 'Test User', role: 'USER' };

      renderAdminRoute();

      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
      expect(screen.queryByTestId('admin-content')).not.toBeInTheDocument();
    });

    it('redirects when user object is null', () => {
      mockIsAuthenticated = true;
      mockUser = null;

      renderAdminRoute();

      expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
      expect(screen.queryByTestId('admin-content')).not.toBeInTheDocument();
    });
  });

  describe('authenticated users with admin roles', () => {
    it('renders children for VENUE_ADMIN role', () => {
      mockIsAuthenticated = true;
      mockUser = { id: 1, email: 'venue@test.com', fullName: 'Venue Admin', role: 'VENUE_ADMIN' };

      renderAdminRoute();

      expect(screen.getByTestId('admin-content')).toBeInTheDocument();
      expect(screen.getByText('Admin Content')).toBeInTheDocument();
    });

    it('renders children for ADMIN role', () => {
      mockIsAuthenticated = true;
      mockUser = { id: 1, email: 'admin@test.com', fullName: 'Admin User', role: 'ADMIN' };

      renderAdminRoute();

      expect(screen.getByTestId('admin-content')).toBeInTheDocument();
      expect(screen.getByText('Admin Content')).toBeInTheDocument();
    });

    it('does not redirect admin users', () => {
      mockIsAuthenticated = true;
      mockUser = { id: 1, email: 'admin@test.com', fullName: 'Admin User', role: 'ADMIN' };

      renderAdminRoute();

      expect(screen.queryByTestId('login-page')).not.toBeInTheDocument();
      expect(screen.queryByTestId('dashboard-page')).not.toBeInTheDocument();
    });

    it('does not call setRedirectPath for admin users', () => {
      mockIsAuthenticated = true;
      mockUser = { id: 1, email: 'admin@test.com', fullName: 'Admin User', role: 'ADMIN' };

      renderAdminRoute();

      expect(mockSetRedirectPath).not.toHaveBeenCalled();
    });
  });
});
