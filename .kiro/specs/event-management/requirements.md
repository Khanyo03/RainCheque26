# Requirements Document

## Introduction

The Event Management module extends the existing read-only event capabilities in RainCheque with full CRUD operations, image upload support, category-based filtering, search with pagination, input validation, and a responsive admin UI. The module enables VENUE_ADMIN and ADMIN users to create, edit, publish, and delete events, while regular users continue to browse and search events through the existing public endpoints.

## Glossary

- **Event_Management_API**: The set of backend REST endpoints under `/api/v1/admin/events` that handle event creation, update, deletion, and image uploads
- **Event_Form_UI**: The frontend React component providing the form interface for creating and editing events
- **Event_List_UI**: The frontend React component displaying a paginated, searchable, filterable list of events for administrators
- **Image_Upload_Service**: The backend service responsible for receiving, validating, storing, and serving event images (banner and thumbnail)
- **Event_Validator**: The backend validation layer that enforces data integrity rules on event creation and update requests
- **Category_Filter**: The mechanism allowing users to filter events by their category field
- **Search_Engine**: The backend query logic that matches events by keyword against title, description, and venue name
- **Authorized_User**: A user with role VENUE_ADMIN or ADMIN who is authenticated via JWT

## Requirements

### Requirement 1: Create Event

**User Story:** As a venue admin, I want to create new events with all required details, so that I can publish them for ticket sales.

#### Acceptance Criteria

1. WHEN an Authorized_User submits a valid event creation request, THE Event_Management_API SHALL persist the event with status DRAFT and return the created event with HTTP 201
2. WHEN an Authorized_User submits an event creation request with missing required fields (title, venue, startTime, endTime, category), THE Event_Validator SHALL reject the request with HTTP 400 and field-level error messages
3. WHEN an unauthenticated user attempts to create an event, THE Event_Management_API SHALL return HTTP 401
4. WHEN a user with role USER attempts to create an event, THE Event_Management_API SHALL return HTTP 403
5. WHEN an Authorized_User submits an event where endTime is before or equal to startTime, THE Event_Validator SHALL reject the request with HTTP 400 and a descriptive error message

### Requirement 2: Read Events (Admin)

**User Story:** As a venue admin, I want to view all my events regardless of status, so that I can manage drafts, published, and cancelled events.

#### Acceptance Criteria

1. WHEN an Authorized_User requests the admin event list, THE Event_Management_API SHALL return a paginated list of all events (including DRAFT, PUBLISHED, CANCELLED, COMPLETED) sorted by creation date descending
2. WHEN an Authorized_User requests a specific event by ID, THE Event_Management_API SHALL return the full event details including metadata and ticket tiers
3. IF the requested event ID does not exist, THEN THE Event_Management_API SHALL return HTTP 404 with an error message

### Requirement 3: Update Event

**User Story:** As a venue admin, I want to update event details, so that I can correct information or change event configuration before and after publishing.

#### Acceptance Criteria

1. WHEN an Authorized_User submits a valid update request for an existing event, THE Event_Management_API SHALL persist the changes and return the updated event with HTTP 200
2. WHEN an Authorized_User submits an update request with invalid data, THE Event_Validator SHALL reject the request with HTTP 400 and field-level error messages
3. WHEN an Authorized_User updates the status of an event to PUBLISHED, THE Event_Management_API SHALL make the event visible on public endpoints
4. WHEN an Authorized_User updates the status of an event to CANCELLED, THE Event_Management_API SHALL remove the event from public listing while preserving the record
5. IF the event to update does not exist, THEN THE Event_Management_API SHALL return HTTP 404

### Requirement 4: Delete Event

**User Story:** As an admin, I want to delete events that are no longer needed, so that the system remains clean and accurate.

#### Acceptance Criteria

1. WHEN an Authorized_User sends a delete request for an existing event, THE Event_Management_API SHALL remove the event and return HTTP 204
2. IF the event to delete does not exist, THEN THE Event_Management_API SHALL return HTTP 404
3. WHEN an unauthenticated user attempts to delete an event, THE Event_Management_API SHALL return HTTP 401
4. WHEN a user with role USER attempts to delete an event, THE Event_Management_API SHALL return HTTP 403

### Requirement 5: Image Upload

**User Story:** As a venue admin, I want to upload banner and thumbnail images for events, so that they display attractively in listings and detail pages.

#### Acceptance Criteria

1. WHEN an Authorized_User uploads a valid image file (JPEG, PNG, or WebP) with size at most 5 MB, THE Image_Upload_Service SHALL store the file in the configured upload directory and return the file URL with HTTP 200
2. WHEN an Authorized_User uploads a file that is not JPEG, PNG, or WebP, THE Image_Upload_Service SHALL reject the upload with HTTP 400 and indicate the allowed file types
3. WHEN an Authorized_User uploads a file exceeding 5 MB, THE Image_Upload_Service SHALL reject the upload with HTTP 400 and indicate the maximum allowed size
4. THE Image_Upload_Service SHALL generate a unique filename for each upload to prevent collisions
5. WHEN an Authorized_User uploads an image and specifies the target field (banner or thumbnail), THE Event_Management_API SHALL update the corresponding URL field on the event

### Requirement 6: Event Categories

**User Story:** As a user, I want to browse events by category, so that I can find events matching my interests.

#### Acceptance Criteria

1. WHEN a user requests the categories list, THE Event_Management_API SHALL return all distinct categories from published events
2. WHEN a user requests events filtered by a specific category, THE Search_Engine SHALL return only published events matching that category with pagination
3. WHEN an Authorized_User creates or updates an event, THE Event_Validator SHALL accept any non-empty string as a category value

### Requirement 7: Event Search

**User Story:** As a user, I want to search events by keyword, so that I can find specific events quickly.

#### Acceptance Criteria

1. WHEN a user submits a search query, THE Search_Engine SHALL match events by title, description, or venue name and return paginated results
2. WHEN a user submits a search query with category and date filters, THE Search_Engine SHALL apply all filters in combination and return matching results
3. WHEN a search query yields no results, THE Search_Engine SHALL return an empty page with HTTP 200
4. THE Search_Engine SHALL perform case-insensitive matching on search queries

### Requirement 8: Pagination

**User Story:** As a user, I want event listings to be paginated, so that pages load quickly and I can navigate through large datasets.

#### Acceptance Criteria

1. THE Event_Management_API SHALL accept page number and page size parameters on all list endpoints with defaults of page 0 and size 10
2. THE Event_Management_API SHALL return pagination metadata (totalElements, totalPages, currentPage, pageSize) in all paginated responses
3. WHEN a user requests a page number beyond the available pages, THE Event_Management_API SHALL return an empty content array with correct pagination metadata

### Requirement 9: Input Validation

**User Story:** As a developer, I want comprehensive validation on all event inputs, so that data integrity is maintained and users receive clear feedback.

#### Acceptance Criteria

1. THE Event_Validator SHALL enforce that title is between 3 and 200 characters
2. THE Event_Validator SHALL enforce that description does not exceed 10000 characters
3. THE Event_Validator SHALL enforce that shortDescription does not exceed 500 characters
4. THE Event_Validator SHALL enforce that startTime is in the future when creating a new event
5. THE Event_Validator SHALL enforce that endTime is after startTime
6. THE Event_Validator SHALL return all validation errors in a single response rather than failing on the first error
7. THE Event_Validator SHALL enforce that category is not blank

### Requirement 10: Responsive Admin UI

**User Story:** As a venue admin, I want the event management interface to work on desktop, tablet, and mobile devices, so that I can manage events from any device.

#### Acceptance Criteria

1. THE Event_Form_UI SHALL render a multi-field form with inputs for title, description, short description, venue selection, start time, end time, category, featured flag, and metadata fields
2. THE Event_Form_UI SHALL display inline validation errors beneath each invalid field
3. THE Event_Form_UI SHALL include image upload controls for banner and thumbnail with preview
4. THE Event_List_UI SHALL display events in a responsive table on desktop (breakpoint 1024px and above) and a card layout on smaller screens
5. THE Event_List_UI SHALL provide search, category filter, and status filter controls
6. THE Event_List_UI SHALL include pagination controls that reflect current page state
7. WHEN the viewport width is below 768px, THE Event_Form_UI SHALL stack form fields vertically with full-width inputs
8. THE Event_Form_UI SHALL disable the submit button and show a loading indicator while the save request is in progress
