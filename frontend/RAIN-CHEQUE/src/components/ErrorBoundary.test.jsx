import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import ErrorBoundary from './ErrorBoundary';

// A component that throws during render when `shouldThrow` is true
function ThrowingChild({ shouldThrow }) {
  if (shouldThrow) {
    throw new Error('Test render error');
  }
  return <div data-testid="child-content">Hello World</div>;
}

describe('ErrorBoundary', () => {
  beforeEach(() => {
    // Suppress React error boundary console output during tests
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders children when no error occurs', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild shouldThrow={false} />
      </ErrorBoundary>
    );

    expect(screen.getByTestId('child-content')).toBeInTheDocument();
    expect(screen.getByText('Hello World')).toBeInTheDocument();
  });

  it('renders fallback UI when a child component throws', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>
    );

    expect(screen.queryByTestId('child-content')).not.toBeInTheDocument();
    expect(screen.getByText('Oops! Something went wrong.')).toBeInTheDocument();
    expect(screen.getByText('Please try again or return to the home page.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /go home/i })).toBeInTheDocument();
  });

  it('"Go Home" link points to "/"', () => {
    render(
      <ErrorBoundary>
        <ThrowingChild shouldThrow={true} />
      </ErrorBoundary>
    );

    const goHomeLink = screen.getByRole('link', { name: /go home/i });
    expect(goHomeLink).toHaveAttribute('href', '/');
  });

  it('"Try Again" button resets error state and re-renders children', async () => {
    const user = userEvent.setup();

    // Use a stateful wrapper so we can control whether the child throws
    let shouldThrow = true;

    function ConditionalThrow() {
      if (shouldThrow) {
        throw new Error('Test render error');
      }
      return <div data-testid="child-content">Recovered</div>;
    }

    const { rerender } = render(
      <ErrorBoundary>
        <ConditionalThrow />
      </ErrorBoundary>
    );

    // Fallback should be showing
    expect(screen.getByText('Oops! Something went wrong.')).toBeInTheDocument();

    // Stop throwing so re-render succeeds
    shouldThrow = false;

    await user.click(screen.getByRole('button', { name: /try again/i }));

    // After clicking Try Again, children should re-render
    expect(screen.getByTestId('child-content')).toBeInTheDocument();
    expect(screen.getByText('Recovered')).toBeInTheDocument();
    expect(screen.queryByText('Oops! Something went wrong.')).not.toBeInTheDocument();
  });

  it('shows fallback again if re-render also throws (no infinite loop)', async () => {
    const user = userEvent.setup();

    function AlwaysThrows() {
      throw new Error('Always fails');
    }

    render(
      <ErrorBoundary>
        <AlwaysThrows />
      </ErrorBoundary>
    );

    // Fallback showing initially
    expect(screen.getByText('Oops! Something went wrong.')).toBeInTheDocument();

    // Click Try Again - child will throw again
    await user.click(screen.getByRole('button', { name: /try again/i }));

    // Fallback should show again without crashing
    expect(screen.getByText('Oops! Something went wrong.')).toBeInTheDocument();
  });
});
