import { render, screen, fireEvent } from '@testing-library/react';
import PaginationControls from './PaginationControls';

describe('PaginationControls', () => {
  const mockOnPageChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders page info text', () => {
      render(
        <PaginationControls currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByText('Page 1 of 5')).toBeInTheDocument();
    });

    it('renders Previous and Next buttons', () => {
      render(
        <PaginationControls currentPage={1} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByLabelText('Previous page')).toBeInTheDocument();
      expect(screen.getByLabelText('Next page')).toBeInTheDocument();
    });

    it('renders nothing when totalPages is 1', () => {
      const { container } = render(
        <PaginationControls currentPage={0} totalPages={1} onPageChange={mockOnPageChange} />
      );
      expect(container.innerHTML).toBe('');
    });

    it('renders nothing when totalPages is 0', () => {
      const { container } = render(
        <PaginationControls currentPage={0} totalPages={0} onPageChange={mockOnPageChange} />
      );
      expect(container.innerHTML).toBe('');
    });

    it('has proper aria-label on navigation', () => {
      render(
        <PaginationControls currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByRole('navigation')).toHaveAttribute('aria-label', 'Pagination');
    });
  });

  describe('button states', () => {
    it('disables Previous button on first page', () => {
      render(
        <PaginationControls currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByLabelText('Previous page')).toBeDisabled();
    });

    it('disables Next button on last page', () => {
      render(
        <PaginationControls currentPage={4} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByLabelText('Next page')).toBeDisabled();
    });

    it('enables both buttons on middle pages', () => {
      render(
        <PaginationControls currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      expect(screen.getByLabelText('Previous page')).not.toBeDisabled();
      expect(screen.getByLabelText('Next page')).not.toBeDisabled();
    });
  });

  describe('navigation', () => {
    it('calls onPageChange with previous page on Previous click', () => {
      render(
        <PaginationControls currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      fireEvent.click(screen.getByLabelText('Previous page'));
      expect(mockOnPageChange).toHaveBeenCalledWith(1);
    });

    it('calls onPageChange with next page on Next click', () => {
      render(
        <PaginationControls currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      fireEvent.click(screen.getByLabelText('Next page'));
      expect(mockOnPageChange).toHaveBeenCalledWith(3);
    });

    it('calls onPageChange when a page number button is clicked', () => {
      render(
        <PaginationControls currentPage={0} totalPages={5} onPageChange={mockOnPageChange} />
      );
      fireEvent.click(screen.getByLabelText('Page 3'));
      expect(mockOnPageChange).toHaveBeenCalledWith(2);
    });
  });

  describe('current page highlight', () => {
    it('highlights the current page button with aria-current', () => {
      render(
        <PaginationControls currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      const currentPageBtn = screen.getByLabelText('Page 3');
      expect(currentPageBtn).toHaveAttribute('aria-current', 'page');
    });

    it('does not set aria-current on non-current pages', () => {
      render(
        <PaginationControls currentPage={2} totalPages={5} onPageChange={mockOnPageChange} />
      );
      const otherPageBtn = screen.getByLabelText('Page 1');
      expect(otherPageBtn).not.toHaveAttribute('aria-current');
    });
  });
});
