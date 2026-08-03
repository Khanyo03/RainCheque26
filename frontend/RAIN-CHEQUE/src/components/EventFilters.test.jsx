import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EventFilters from './EventFilters';

describe('EventFilters', () => {
  const mockOnFilterChange = vi.fn();
  const categories = ['Music', 'Sports', 'Theatre', 'Comedy'];

  beforeEach(() => {
    vi.clearAllMocks();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('rendering', () => {
    it('renders search input', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      expect(screen.getByLabelText('Search events')).toBeInTheDocument();
    });

    it('renders category dropdown with all categories', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const categorySelect = screen.getByLabelText('Filter by category');
      expect(categorySelect).toBeInTheDocument();
      expect(screen.getByText('All Categories')).toBeInTheDocument();
      expect(screen.getByText('Music')).toBeInTheDocument();
      expect(screen.getByText('Sports')).toBeInTheDocument();
      expect(screen.getByText('Theatre')).toBeInTheDocument();
      expect(screen.getByText('Comedy')).toBeInTheDocument();
    });

    it('renders status dropdown with all status options', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const statusSelect = screen.getByLabelText('Filter by status');
      expect(statusSelect).toBeInTheDocument();
      expect(screen.getByText('All Statuses')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
      expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
      expect(screen.getByText('CANCELLED')).toBeInTheDocument();
      expect(screen.getByText('COMPLETED')).toBeInTheDocument();
    });

    it('renders with initial filter values', () => {
      render(
        <EventFilters
          onFilterChange={mockOnFilterChange}
          categories={categories}
          initialFilters={{ search: 'concert', category: 'Music', status: 'PUBLISHED' }}
        />
      );
      expect(screen.getByLabelText('Search events')).toHaveValue('concert');
      expect(screen.getByLabelText('Filter by category')).toHaveValue('Music');
      expect(screen.getByLabelText('Filter by status')).toHaveValue('PUBLISHED');
    });
  });

  describe('search with debounce', () => {
    it('does not call onFilterChange immediately on search input', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const searchInput = screen.getByLabelText('Search events');

      fireEvent.change(searchInput, { target: { value: 'test' } });

      // Should not have been called yet (debounce)
      expect(mockOnFilterChange).not.toHaveBeenCalled();
    });

    it('calls onFilterChange after debounce delay (300ms)', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const searchInput = screen.getByLabelText('Search events');

      fireEvent.change(searchInput, { target: { value: 'concert' } });

      act(() => {
        vi.advanceTimersByTime(300);
      });

      expect(mockOnFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'concert' })
      );
    });

    it('debounces rapid typing', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const searchInput = screen.getByLabelText('Search events');

      fireEvent.change(searchInput, { target: { value: 'c' } });
      act(() => { vi.advanceTimersByTime(100); });
      
      fireEvent.change(searchInput, { target: { value: 'co' } });
      act(() => { vi.advanceTimersByTime(100); });
      
      fireEvent.change(searchInput, { target: { value: 'con' } });
      act(() => { vi.advanceTimersByTime(300); });

      // Only the last value should be emitted
      expect(mockOnFilterChange).toHaveBeenCalledTimes(1);
      expect(mockOnFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ search: 'con' })
      );
    });
  });

  describe('category filter', () => {
    it('emits filter change immediately on category selection', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const categorySelect = screen.getByLabelText('Filter by category');

      fireEvent.change(categorySelect, { target: { value: 'Music' } });

      expect(mockOnFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ category: 'Music' })
      );
    });
  });

  describe('status filter', () => {
    it('emits filter change immediately on status selection', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      const statusSelect = screen.getByLabelText('Filter by status');

      fireEvent.change(statusSelect, { target: { value: 'PUBLISHED' } });

      expect(mockOnFilterChange).toHaveBeenCalledWith(
        expect.objectContaining({ status: 'PUBLISHED' })
      );
    });
  });

  describe('combined filters', () => {
    it('includes all filter values in the callback', () => {
      render(
        <EventFilters
          onFilterChange={mockOnFilterChange}
          categories={categories}
          initialFilters={{ search: 'rock' }}
        />
      );

      const categorySelect = screen.getByLabelText('Filter by category');
      fireEvent.change(categorySelect, { target: { value: 'Music' } });

      expect(mockOnFilterChange).toHaveBeenCalledWith({
        search: 'rock',
        category: 'Music',
        status: '',
      });
    });
  });

  describe('accessibility', () => {
    it('has proper labels on all filter controls', () => {
      render(<EventFilters onFilterChange={mockOnFilterChange} categories={categories} />);
      expect(screen.getByLabelText('Search events')).toBeInTheDocument();
      expect(screen.getByLabelText('Filter by category')).toBeInTheDocument();
      expect(screen.getByLabelText('Filter by status')).toBeInTheDocument();
    });
  });
});
