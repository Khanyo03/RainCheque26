import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ImageUploader from './ImageUploader';

describe('ImageUploader', () => {
  const mockOnUpload = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('rendering', () => {
    it('renders with default label', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      expect(screen.getByText('Upload Image')).toBeInTheDocument();
    });

    it('renders with custom label', () => {
      render(<ImageUploader onUpload={mockOnUpload} label="Banner Image" />);
      expect(screen.getByText('Banner Image')).toBeInTheDocument();
    });

    it('renders drag-and-drop zone with instructions', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      expect(screen.getByText(/drag and drop/i)).toBeInTheDocument();
      expect(screen.getByText(/click to browse/i)).toBeInTheDocument();
    });

    it('displays accepted formats info', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      expect(screen.getByText(/JPEG, PNG, or WebP/i)).toBeInTheDocument();
    });

    it('renders with proper aria label for accessibility', () => {
      render(<ImageUploader onUpload={mockOnUpload} label="Banner" />);
      const dropZone = screen.getByRole('button');
      expect(dropZone).toHaveAttribute('aria-label', expect.stringContaining('Banner'));
    });

    it('is keyboard navigable (has tabIndex)', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      const dropZone = screen.getByRole('button');
      expect(dropZone).toHaveAttribute('tabindex', '0');
    });

    it('shows current image when currentImageUrl is provided', () => {
      render(
        <ImageUploader onUpload={mockOnUpload} currentImageUrl="http://example.com/image.jpg" />
      );
      const previewImg = screen.getByAltText('Upload preview');
      expect(previewImg).toHaveAttribute('src', 'http://example.com/image.jpg');
    });
  });

  describe('file validation', () => {
    it('shows error for invalid file type', async () => {
      render(<ImageUploader onUpload={mockOnUpload} />);

      const file = new File(['content'], 'doc.pdf', { type: 'application/pdf' });
      const input = document.querySelector('input[type="file"]');
      
      fireEvent.change(input, { target: { files: [file] } });

      expect(screen.getByRole('alert')).toHaveTextContent(/invalid file type/i);
      expect(mockOnUpload).not.toHaveBeenCalled();
    });

    it('shows error for file exceeding 5 MB', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);

      // Create a file with size > 5 MB
      const largeContent = new Array(6 * 1024 * 1024).fill('a').join('');
      const file = new File([largeContent], 'large.jpg', { type: 'image/jpeg' });
      const input = document.querySelector('input[type="file"]');

      fireEvent.change(input, { target: { files: [file] } });

      expect(screen.getByRole('alert')).toHaveTextContent(/file too large/i);
      expect(mockOnUpload).not.toHaveBeenCalled();
    });

    it('accepts valid JPEG file', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);

      const file = new File(['image-data'], 'photo.jpg', { type: 'image/jpeg' });
      const input = document.querySelector('input[type="file"]');

      fireEvent.change(input, { target: { files: [file] } });

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
      expect(mockOnUpload).toHaveBeenCalledWith(file);
    });

    it('accepts valid PNG file', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);

      const file = new File(['image-data'], 'photo.png', { type: 'image/png' });
      const input = document.querySelector('input[type="file"]');

      fireEvent.change(input, { target: { files: [file] } });

      expect(mockOnUpload).toHaveBeenCalledWith(file);
    });

    it('accepts valid WebP file', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);

      const file = new File(['image-data'], 'photo.webp', { type: 'image/webp' });
      const input = document.querySelector('input[type="file"]');

      fireEvent.change(input, { target: { files: [file] } });

      expect(mockOnUpload).toHaveBeenCalledWith(file);
    });
  });

  describe('drag and drop', () => {
    it('highlights drop zone on drag over', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      const dropZone = screen.getByRole('button');

      fireEvent.dragOver(dropZone, { dataTransfer: { files: [] } });

      expect(dropZone.className).toContain('bg-red-700');
    });

    it('removes highlight on drag leave', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      const dropZone = screen.getByRole('button');

      fireEvent.dragOver(dropZone, { dataTransfer: { files: [] } });
      fireEvent.dragLeave(dropZone, { dataTransfer: { files: [] } });

      expect(dropZone.className).not.toContain('bg-red-700');
    });

    it('handles file drop with valid file', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      const dropZone = screen.getByRole('button');

      const file = new File(['image-data'], 'dropped.png', { type: 'image/png' });
      fireEvent.drop(dropZone, {
        dataTransfer: { files: [file] },
      });

      expect(mockOnUpload).toHaveBeenCalledWith(file);
    });

    it('validates dropped files', () => {
      render(<ImageUploader onUpload={mockOnUpload} />);
      const dropZone = screen.getByRole('button');

      const file = new File(['content'], 'doc.txt', { type: 'text/plain' });
      fireEvent.drop(dropZone, {
        dataTransfer: { files: [file] },
      });

      expect(screen.getByRole('alert')).toHaveTextContent(/invalid file type/i);
      expect(mockOnUpload).not.toHaveBeenCalled();
    });
  });

  describe('preview', () => {
    it('shows remove button when preview is displayed', () => {
      render(
        <ImageUploader onUpload={mockOnUpload} currentImageUrl="http://example.com/img.jpg" />
      );
      expect(screen.getByLabelText('Remove image')).toBeInTheDocument();
    });

    it('clears preview when remove button is clicked', async () => {
      render(
        <ImageUploader onUpload={mockOnUpload} currentImageUrl="http://example.com/img.jpg" />
      );

      const removeBtn = screen.getByLabelText('Remove image');
      fireEvent.click(removeBtn);

      expect(screen.queryByAltText('Upload preview')).not.toBeInTheDocument();
      expect(screen.getByText(/drag and drop/i)).toBeInTheDocument();
    });
  });
});
