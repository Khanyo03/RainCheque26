# Implementation Plan: Replace Mock Data with API

## Overview

Remove all inline mock data and hardcoded fallbacks from four React page components (EventDetailPage, TicketsPage, DashboardPage, ProfilePage) and replace them with real API calls via the existing Axios service layer or localStorage reads. Each page transitions from a "try API, fall back to mock" pattern to a "call API, show loading/error states" pattern while preserving the existing UI layout.

## Tasks

- [x] 1. Refactor EventDetailPage to use real API data only
  - [x] 1.1 Remove mock data and add route param validation in EventDetailPage
    - Delete the entire `mockEvents` object (~130 lines) and `usingMockData` state variable
    - Remove the mock data indicator UI (`<div>Using demo data...`)
    - Add route param validation: if `id` does not match `/^\d+$/`, set error state immediately without calling the API
    - Rewrite `fetchEventDetails()` to call `api.get(`/events/${id}`)` directly with no inner try/catch fallback to mock data
    - Distinguish 404 from other errors: check `err.response?.status === 404` to render "Event not found" message
    - For non-404 errors, display the generic ErrorAlert component
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 5.1, 5.5_

  - [x] 1.2 Remove mock fallbacks from booking and payment handlers in EventDetailPage
    - Remove the inner try/catch in `handleBookNow` that substitutes a mock booking object on API failure
    - Remove the inner try/catch in `handlePayment` that simulates a successful payment on API failure
    - On booking/payment API failure, set error state with a descriptive message
    - _Requirements: 5.8_

  - [x] 1.3 Write unit tests for EventDetailPage API integration
    - Test that EventDetailPage renders event data from a mocked API response
    - Test that a loading spinner displays during fetch
    - Test that "Event not found" message renders on 404 response
    - Test that generic error displays on non-404 failure
    - Test that invalid route param (e.g. "abc", "-1", "0") shows error without API call
    - Verify no `mockEvents` or `usingMockData` references exist in the component
    - _Requirements: 1.1–1.7, 5.1, 5.5_

- [x] 2. Refactor TicketsPage to use real API data only
  - [x] 2.1 Remove mock data and mock download logic in TicketsPage
    - Delete the `mockTickets` array inside `fetchTickets()`
    - Remove the inner try/catch that assigns mock tickets on API failure
    - Rewrite `fetchTickets()` to call `api.get('/tickets')` with error propagation to the `error` state
    - Remove mock download logic in `handleDownload` (the text file fallback)
    - Remove mock download logic in `handleDownloadAll` (the text file fallback per booking)
    - Keep the real PDF download logic in `handleDownload` and `handleDownloadAll`
    - Add download error classification: 404 → "Ticket not found", 401 → "Not authorized", 403 → "Access denied", timeout → "Request timed out", other → "Server error: {status}"
    - Map `ticket.qrSecret` to a QR code URL when passing to TicketCard: `https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(ticket.qrSecret)}`
    - Ensure empty ticket array renders the existing empty state with a link to browse events
    - Disable repeated download clicks while a download is in progress (use `downloading` state)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 5.2, 5.6_

  - [x] 2.2 Write unit tests for TicketsPage API integration
    - Test that TicketsPage renders tickets from a mocked API response
    - Test empty state when API returns an empty array
    - Test error state when API call fails
    - Test that clicking download triggers the correct API call with blob responseType
    - Test download error messages for each status code (404, 401, 403, timeout, other)
    - Verify no `mockTickets` array exists in the component
    - _Requirements: 2.1–2.11, 5.2, 5.6_

- [x] 3. Checkpoint - Verify EventDetailPage and TicketsPage changes
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Refactor DashboardPage to use localStorage and API data
  - [x] 4.1 Convert DashboardPage from stateless to stateful with real data
    - Add `useState` and `useEffect` imports
    - Add `useNavigate` from react-router-dom
    - Read user from `localStorage.getItem('user')` on mount with JSON.parse and validation
    - If user is null, empty, or invalid JSON → `navigate('/login')` and return null
    - Add `loading` and `error` state for the tickets API call
    - Call `api.get('/tickets')` on mount to get ticket count (`response.data.length`)
    - Remove the hardcoded `user` object with literal fullName, email, memberSince values
    - Remove the hardcoded "Upcoming Events" (3) and "Wishlist" (5) stats
    - Display only "Tickets Purchased" stat derived from the API response length
    - Show LoadingSpinner in the stats section while fetching
    - On API error, show 0 for ticket count and display ErrorAlert in stats section
    - Keep welcome header using `user.fullName` from localStorage
    - Keep `memberSince` if present in localStorage user, otherwise hide it
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 5.3_

  - [x] 4.2 Write unit tests for DashboardPage
    - Test that DashboardPage reads user from localStorage and displays fullName
    - Test that DashboardPage fetches ticket count from API and displays it
    - Test redirect to /login when localStorage has no user
    - Test redirect to /login when localStorage has invalid JSON
    - Test that error state shows 0 for ticket count
    - Verify no hardcoded user object exists
    - _Requirements: 3.1–3.7, 5.3_

- [x] 5. Refactor ProfilePage to use localStorage data with validation
  - [x] 5.1 Implement ProfilePage localStorage integration and form validation
    - Add `useEffect` to read `localStorage.getItem('user')` on mount and parse JSON
    - Add `useNavigate` from react-router-dom
    - If localStorage user is not present, null, empty string, or invalid JSON → redirect to /login
    - Pre-populate `fullName` and `email` from parsed user, set `phone` to empty string
    - Remove hardcoded initial values ('John Doe', 'john@example.com', '+1 234 567 8900')
    - Add `validationErrors` state object for inline field errors
    - Validate `fullName`: must be 1–100 characters; show inline error if empty or >100
    - Validate `email`: must match basic email regex; show inline error if invalid
    - On valid submit: update localStorage user object merging new `fullName`, `email`, `phone` while preserving existing fields (e.g. `role`)
    - Add `successMessage` state; on successful save, display success message and auto-dismiss after 3 seconds
    - If validation fails, do NOT update localStorage and show inline errors on the relevant fields
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 5.4_

  - [x] 5.2 Write unit tests for ProfilePage
    - Test that ProfilePage reads user from localStorage and pre-populates form
    - Test redirect to /login when localStorage is missing/invalid
    - Test validation error on empty fullName
    - Test validation error on fullName > 100 characters
    - Test validation error on invalid email format
    - Test successful save updates localStorage preserving existing fields
    - Test success message displays and auto-dismisses
    - Verify no hardcoded placeholder values exist
    - _Requirements: 4.1–4.6, 5.4_

- [x] 6. Final checkpoint - Verify all pages work with real API data
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- The design has no Correctness Properties section, so property-based tests are not applicable — unit tests with mocked APIs cover all verification needs
- The existing `src/services/api.js` Axios instance is used as-is (no modifications needed)
- The `src/mocks/` directory is retained for dev-time MSW mocking and is not imported in production
- QR codes are generated client-side from the backend's `qrSecret` field using a public QR API
- All UI layout (Tailwind classes, component nesting, element hierarchy) remains unchanged

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "4.1", "5.1"] },
    { "id": 1, "tasks": ["1.2", "2.1"] },
    { "id": 2, "tasks": ["1.3", "2.2", "4.2", "5.2"] }
  ]
}
```
