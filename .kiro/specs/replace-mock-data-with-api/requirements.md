# Requirements Document

## Introduction

Replace all mock and hardcoded data in the RainCheque React frontend with real API calls to the Spring Boot backend. Pages that already use real API calls (EventsPage, LoginPage, RegisterPage) remain unchanged. Pages with mock data (EventDetailPage, TicketsPage, DashboardPage, ProfilePage) will be updated to use the existing Axios service layer (`src/services/api.js`) for backend communication, or localStorage for user profile data where no backend endpoint exists.

## Glossary

- **Frontend**: The React single-page application located at `frontend/RAIN-CHEQUE/`
- **Backend**: The Spring Boot REST API located at `backend-spring/rain-cheque-backend/`
- **API_Service**: The Axios-based HTTP client configured in `src/services/api.js` with base URL, auth token interceptor, and error handling
- **EventDetailPage**: The React page component that displays a single event's details and ticket tiers
- **TicketsPage**: The React page component that displays the authenticated user's purchased tickets
- **DashboardPage**: The React page component that displays the user's welcome message and statistics
- **ProfilePage**: The React page component that displays and allows editing of the user's personal information
- **LocalStorage_User**: The user object stored in `localStorage` under the key `user` after login, containing `fullName`, `email`, and other profile fields
- **Loading_State**: A boolean UI state that displays a spinner or skeleton while data is being fetched
- **Error_State**: A UI state that displays an error message when an API call fails

## Requirements

### Requirement 1: Event Detail Page API Integration

**User Story:** As an event attendee, I want to view real event details from the backend, so that I see accurate and up-to-date event information.

#### Acceptance Criteria

1. WHEN the EventDetailPage mounts with a route parameter `id`, THE API_Service SHALL send a GET request to `/events/{id}` to fetch event details within 1 second of component mount
2. WHILE the GET request is in progress, THE EventDetailPage SHALL display a Loading_State indicator that remains visible until the response is received or a timeout of 30 seconds elapses
3. WHEN the GET request returns a successful response, THE EventDetailPage SHALL render the event data from the response including title, description, venueName, venueCity, venueAddress, startTime, endTime, category, bannerImageUrl, featured status, ticketTiers (with name, description, price, remainingQuantity, totalQuantity, maxPerUser, and benefits for each tier), and metadata
4. IF the GET request returns an error response with an HTTP status other than 404, THEN THE EventDetailPage SHALL display an Error_State message indicating the failure reason and the event content area SHALL remain empty with no fallback data rendered
5. WHEN the EventDetailPage renders event data, THE EventDetailPage SHALL not contain any inline mock data objects, `mockEvents` definitions, or `usingMockData` state variables that could serve as a fallback data source
6. IF the GET request returns a 404 status, THEN THE EventDetailPage SHALL display a distinct "Event not found" message that is visually differentiated from the generic Error_State
7. IF the route parameter `id` is not a valid positive integer, THEN THE EventDetailPage SHALL display an Error_State message indicating an invalid event identifier without sending a request to the backend

### Requirement 2: Tickets Page API Integration

**User Story:** As a ticket holder, I want to view my real purchased tickets from the backend, so that I can see my actual booking history and download valid ticket PDFs.

#### Acceptance Criteria

1. WHEN the TicketsPage mounts, THE API_Service SHALL send an authenticated GET request to `/tickets` to fetch the user's tickets
2. WHILE the GET request is in progress, THE TicketsPage SHALL display a Loading_State indicator
3. WHEN the GET request returns a successful response containing one or more tickets, THE TicketsPage SHALL render the list of tickets from the response data
4. IF the GET request returns an error response, THEN THE TicketsPage SHALL display an Error_State message indicating the request failed
5. WHEN a user clicks download on a single ticket, THE API_Service SHALL send an authenticated GET request to `/tickets/{ticketId}/download` with response type `blob`
6. WHEN the download response is received with content type `application/pdf`, THE TicketsPage SHALL trigger a browser file download using the filename from the Content-Disposition response header, or a default filename of `ticket-{ticketId}.pdf` if the header is absent
7. WHEN a user clicks "Download All", THE API_Service SHALL send authenticated GET requests to `/tickets/booking/{bookingId}/download` for each distinct booking represented in the ticket list
8. WHEN the TicketsPage renders ticket data, THE TicketsPage SHALL not use any inline mock ticket arrays as a fallback data source
9. IF the download request returns an error, THEN THE TicketsPage SHALL display an error message indicating the cause: "not found" for 404, "not authorized" for 401, "access denied" for 403, "request timed out" for timeout, or "server error" with the status code for other failures
10. WHEN the GET request returns a successful response containing zero tickets, THE TicketsPage SHALL display an empty state message indicating no tickets have been purchased and a link to browse events
11. WHILE a single-ticket or batch download request is in progress, THE TicketsPage SHALL display a downloading indicator on the affected ticket or action button and disable repeated download clicks

### Requirement 3: Dashboard Page Data from LocalStorage

**User Story:** As a logged-in user, I want to see my real name and membership information on the dashboard, so that the page reflects my actual account data.

#### Acceptance Criteria

1. WHEN the DashboardPage mounts, THE DashboardPage SHALL read the user object from LocalStorage_User and display the `fullName` value in the welcome header and the `email` value as the user's email
2. WHEN the DashboardPage mounts, THE API_Service SHALL send an authenticated GET request to `/tickets` and THE DashboardPage SHALL display the total number of items in the returned tickets array as the "Tickets Purchased" statistic
3. WHILE the GET request to `/tickets` is in progress, THE DashboardPage SHALL display a Loading_State indicator in the statistics section
4. IF the GET request to `/tickets` returns an error response, THEN THE DashboardPage SHALL display an Error_State message in the statistics section and show 0 as the ticket count
5. THE DashboardPage SHALL remove the hardcoded "Upcoming Events" and "Wishlist" statistics that have no backend data source, displaying only the "Tickets Purchased" statistic derived from the API response
6. IF the LocalStorage_User is not present or is empty, THEN THE DashboardPage SHALL redirect the user to the login page
7. WHEN the DashboardPage renders user data, THE DashboardPage SHALL not use any hardcoded user objects as a data source

### Requirement 4: Profile Page Data from LocalStorage

**User Story:** As a logged-in user, I want my profile form to be pre-populated with my real account data, so that I can see and edit my actual information.

#### Acceptance Criteria

1. WHEN the ProfilePage mounts, THE ProfilePage SHALL read the user object from LocalStorage_User (stored under the key `user`) and parse it as JSON to pre-populate the form fields for `fullName` and `email`
2. WHEN the LocalStorage_User contains `fullName` and `email` fields, THE ProfilePage SHALL set those as the initial values of the corresponding form inputs, and SHALL set the `phone` field to an empty string since it is not stored in LocalStorage_User
3. WHEN the ProfilePage renders form data, THE ProfilePage SHALL not use any hardcoded placeholder values like "John Doe" or "john@example.com" as initial form state
4. IF the LocalStorage_User is not present, is null, is an empty string, or contains invalid JSON, THEN THE ProfilePage SHALL redirect the user to the login page
5. WHEN the user submits the profile form with a non-empty `fullName` (1 to 100 characters) and a valid `email`, THE ProfilePage SHALL update the LocalStorage_User with the new `fullName`, `email`, and `phone` values while preserving any existing fields (such as `role`), and SHALL display a visible success message for at least 3 seconds
6. IF the user submits the profile form with an empty `fullName` or an invalid `email` format, THEN THE ProfilePage SHALL display an inline validation error on the corresponding field and SHALL NOT update LocalStorage_User

### Requirement 5: Remove All Mock Data Fallbacks

**User Story:** As a developer, I want mock data and fallback patterns removed from all pages, so that the application only displays real data and surfaces errors clearly.

#### Acceptance Criteria

1. THE EventDetailPage SHALL not contain any inline `mockEvents` object definitions or any `try/catch` logic that assigns mock data to component state when the API call fails
2. THE TicketsPage SHALL not contain any inline `mockTickets` array definitions or any `try/catch` logic that assigns mock data to component state when the API call fails
3. THE DashboardPage SHALL not contain any hardcoded user data objects with literal values for `fullName`, `email`, or `memberSince` fields
4. THE ProfilePage SHALL not contain any hardcoded form initial values that do not come from LocalStorage_User
5. THE EventDetailPage SHALL not contain any `usingMockData` state variable or mock data indicator UI elements
6. THE TicketsPage SHALL not contain any mock download logic that generates a text file as a substitute for a real PDF download from the API
7. IF any page's API request fails, THEN THE Frontend SHALL display the Error_State component with a message indicating the type of failure, instead of silently falling back to mock data or displaying stale content
8. THE EventDetailPage SHALL not contain any `try/catch` logic in booking or payment handlers that substitutes mock response objects when the API call fails
9. THE `src/mocks/` directory containing MSW browser.js and handlers.js files SHALL be retained for development-only API mocking and SHALL not be imported or activated in production builds

### Requirement 6: Preserve Existing UI Layout

**User Story:** As a product owner, I want the UI to remain visually unchanged after the data integration, so that users have a consistent experience.

#### Acceptance Criteria

1. WHEN real data with the same values as the previous mock data is rendered on any page, THE Frontend SHALL produce identical HTML element hierarchy, Tailwind CSS class assignments, and component nesting order as the current implementation
2. THE EventDetailPage SHALL continue to display the booking flow UI (tier selection, quantity selector, payment step) using the same component structure, and SHALL map API response fields to the same template positions previously occupied by mock data fields
3. THE TicketsPage SHALL continue to render each ticket using the existing TicketCard component, passing the same props (ticket code, status, price, seat, tier name, event title, venue, date, QR code) as the current mock data structure provides
4. THE DashboardPage SHALL continue to display the welcome header (user full name, member since), the 3-column stats grid (Upcoming Events, Tickets Purchased, Wishlist), and the 3-column quick actions section with identical Tailwind CSS classes and element ordering
5. THE ProfilePage SHALL continue to display the personal information form (full name, email, phone fields) and the password change form (current password, new password, confirm password fields) using the same input layout, icon placement, and Tailwind CSS classes
6. WHILE data is being fetched, THE Frontend SHALL display the LoadingSpinner component styled consistently with the existing Tailwind CSS theme, and IF an error occurs, THEN THE Frontend SHALL display the ErrorAlert component without altering the surrounding page layout or CSS classes
7. THE Frontend SHALL NOT introduce new CSS files, remove existing Tailwind CSS utility classes from rendered elements, or add external stylesheets as part of the data integration changes
