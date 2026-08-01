import { render } from '@testing-library/react';
import { vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';

const mockLogin = vi.fn();

vi.mock('../context/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
  }),
}));

vi.mock('../services/api', () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import LoginPage from './LoginPage';

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the login form with empty email and password fields', () => {
    const { container } = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const emailInput = container.querySelector('input[name="email"]');
    const passwordInput = container.querySelector('input[name="password"]');

    expect(emailInput).toBeInTheDocument();
    expect(emailInput).toHaveValue('');

    expect(passwordInput).toBeInTheDocument();
    expect(passwordInput).toHaveValue('');
  });

  it('does not pre-fill email or password from any source', () => {
    // Ensure localStorage has no effect on initial form state
    localStorage.setItem('email', 'stored@example.com');
    localStorage.setItem('password', 'storedpass');

    const { container } = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    );

    const emailInput = container.querySelector('input[name="email"]');
    const passwordInput = container.querySelector('input[name="password"]');

    expect(emailInput).toHaveValue('');
    expect(passwordInput).toHaveValue('');

    localStorage.removeItem('email');
    localStorage.removeItem('password');
  });
});
