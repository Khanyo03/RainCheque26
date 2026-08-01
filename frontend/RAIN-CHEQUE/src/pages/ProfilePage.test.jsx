import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProfilePage from './ProfilePage';

// Mock react-router-dom's useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  const renderProfilePage = () => {
    return render(
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    );
  };

  describe('reads user from localStorage and pre-populates form', () => {
    it('pre-populates fullName and email from localStorage user', () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', role: 'USER' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      expect(screen.getByDisplayValue('Jane Smith')).toBeInTheDocument();
      expect(screen.getByDisplayValue('jane@example.com')).toBeInTheDocument();
    });

    it('sets phone field to empty string', () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', phone: '+1 555 000' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const phoneInput = document.querySelector('input[name="phone"]');
      expect(phoneInput.value).toBe('');
    });
  });

  describe('redirect to /login when localStorage is missing/invalid', () => {
    it('redirects to /login when localStorage has no user key', () => {
      renderProfilePage();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is empty string', () => {
      localStorage.setItem('user', '');

      renderProfilePage();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is whitespace only', () => {
      localStorage.setItem('user', '   ');

      renderProfilePage();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user is invalid JSON', () => {
      localStorage.setItem('user', 'not valid json{');

      renderProfilePage();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('redirects to /login when localStorage user parses to null', () => {
      localStorage.setItem('user', 'null');

      renderProfilePage();

      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  describe('validation error on empty fullName', () => {
    it('shows validation error when fullName is cleared and form is submitted', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const fullNameInput = screen.getByDisplayValue('Jane Smith');
      await userEvent.clear(fullNameInput);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Full name is required.')).toBeInTheDocument();
    });

    it('shows validation error when fullName is only whitespace', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const fullNameInput = screen.getByDisplayValue('Jane Smith');
      await userEvent.clear(fullNameInput);
      await userEvent.type(fullNameInput, '   ');

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Full name is required.')).toBeInTheDocument();
    });
  });

  describe('validation error on fullName > 100 characters', () => {
    it('shows validation error when fullName exceeds 100 characters', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const fullNameInput = screen.getByDisplayValue('Jane Smith');
      await userEvent.clear(fullNameInput);
      await userEvent.type(fullNameInput, 'A'.repeat(101));

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Full name must be 100 characters or fewer.')).toBeInTheDocument();
    });
  });

  describe('validation error on invalid email format', () => {
    it('shows validation error when email is empty', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const emailInput = screen.getByDisplayValue('jane@example.com');
      await userEvent.clear(emailInput);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Please enter a valid email address.')).toBeInTheDocument();
    });

    it('shows validation error for email without @ symbol', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const emailInput = screen.getByDisplayValue('jane@example.com');
      await userEvent.clear(emailInput);
      await userEvent.type(emailInput, 'notanemail');

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Please enter a valid email address.')).toBeInTheDocument();
    });

    it('shows validation error for email without domain extension', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const emailInput = screen.getByDisplayValue('jane@example.com');
      await userEvent.clear(emailInput);
      await userEvent.type(emailInput, 'user@domain');

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Please enter a valid email address.')).toBeInTheDocument();
    });
  });

  describe('successful save updates localStorage preserving existing fields', () => {
    it('updates localStorage with new values while preserving role', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', role: 'ADMIN' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const fullNameInput = screen.getByDisplayValue('Jane Smith');
      await userEvent.clear(fullNameInput);
      await userEvent.type(fullNameInput, 'Jane Updated');

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      const stored = JSON.parse(localStorage.getItem('user'));
      expect(stored.fullName).toBe('Jane Updated');
      expect(stored.email).toBe('jane@example.com');
      expect(stored.role).toBe('ADMIN');
      expect(stored.phone).toBe('');
    });

    it('does not update localStorage when validation fails', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com', role: 'USER' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const fullNameInput = screen.getByDisplayValue('Jane Smith');
      await userEvent.clear(fullNameInput);

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      const stored = JSON.parse(localStorage.getItem('user'));
      expect(stored.fullName).toBe('Jane Smith');
    });
  });

  describe('success message displays and auto-dismisses', () => {
    it('shows success message after valid save', async () => {
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      const saveButton = screen.getByRole('button', { name: /save changes/i });
      await userEvent.click(saveButton);

      expect(screen.getByText('Profile updated successfully.')).toBeInTheDocument();
    });

    it('auto-dismisses success message after 3 seconds', async () => {
      vi.useFakeTimers();
      const user = { fullName: 'Jane Smith', email: 'jane@example.com' };
      localStorage.setItem('user', JSON.stringify(user));

      render(
        <MemoryRouter>
          <ProfilePage />
        </MemoryRouter>
      );

      // Submit form using fireEvent (compatible with fake timers)
      const saveButton = screen.getByRole('button', { name: /save changes/i });
      fireEvent.click(saveButton);

      // Success message should be visible
      expect(screen.getByText('Profile updated successfully.')).toBeInTheDocument();

      // Advance time by 3 seconds
      act(() => {
        vi.advanceTimersByTime(3000);
      });

      // Success message should be dismissed
      expect(screen.queryByText('Profile updated successfully.')).not.toBeInTheDocument();

      vi.useRealTimers();
    });
  });

  describe('no hardcoded placeholder values exist', () => {
    it('does not contain hardcoded "John Doe" as initial value', () => {
      const user = { fullName: 'Test User', email: 'test@test.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      expect(screen.queryByDisplayValue('John Doe')).not.toBeInTheDocument();
    });

    it('does not contain hardcoded "john@example.com" as initial value', () => {
      const user = { fullName: 'Test User', email: 'test@test.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      expect(screen.queryByDisplayValue('john@example.com')).not.toBeInTheDocument();
    });

    it('does not contain hardcoded "+1 234 567 8900" as initial value', () => {
      const user = { fullName: 'Test User', email: 'test@test.com' };
      localStorage.setItem('user', JSON.stringify(user));

      renderProfilePage();

      expect(screen.queryByDisplayValue('+1 234 567 8900')).not.toBeInTheDocument();
    });
  });
});
