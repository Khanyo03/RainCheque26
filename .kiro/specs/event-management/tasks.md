# Implementation Plan: Event Management

## Overview

Implement full CRUD event management with image upload, category filtering, combined search with pagination, and a responsive admin UI. The backend adds an `AdminEventController` at `/api/v1/admin/events` with role-restricted access, an `ImageUploadService`, custom validation annotations, and extended repository queries. The frontend adds two lazy-loaded admin pages (event list and event form), shared components (AdminRoute, ImageUploader, PaginationControls, EventFilters), and an `adminEventService` API layer.

## Tasks

- [x] 1. Backend foundation: DTOs, validation, and security configuration
  - [x] 1.1 Create EventRequest record with Jakarta validation annotations and custom @ValidTimeRange annotation
    - Create `EventRequest.java` record in `com.raincheque.dto` with @NotBlank, @Size, @NotNull, @Future constraints
    - Create `ValidTimeRange` annotation and `TimeRangeValidator` implementation in `com.raincheque.validation`
    - Create `StatusRequest.java` and `ImageUploadResponse.java` records in `com.raincheque.dto`
    - _Requirements: 1.2, 1.5, 9.1, 9.2, 9.3, 9.4, 9.5, 9.7_

  - [x] 1.2 Update SecurityConfig to restrict /api/v1/admin/** to VENUE_ADMIN and ADMIN roles and permit /uploads/**
    - Add `.requestMatchers("/api/v1/admin/**").hasAnyRole("VENUE_ADMIN", "ADMIN")` before the existing `.anyRequest().authenticated()` rule
    - Add `.requestMatchers("/uploads/**").permitAll()` to permit static image access
    - _Requirements: 1.3, 1.4, 4.3, 4.4_

  - [x] 1.3 Configure static resource handler for /uploads/** serving from file system
    - Create `WebConfig.java` implementing `WebMvcConfigurer` in `com.raincheque.config`
    - Map `/uploads/**` to `file:${app.upload.dir}/` via `addResourceHandlers`
    - _Requirements: 5.1_

- [x] 2. Backend core: ImageUploadService and EventRepository extensions
  - [x] 2.1 Implement ImageUploadService
    - Create `ImageUploadService.java` in `com.raincheque.service`
    - Validate content type (image/jpeg, image/png, image/webp) and file size (≤ 5 MB)
    - Generate UUID-based unique filenames, store to `${app.upload.dir}/events/`
    - Create custom exceptions: `InvalidFileTypeException`, `FileSizeLimitExceededException`, `FileStorageException`
    - Register exceptions in `GlobalExceptionHandler`
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [x] 2.2 Write property test for image type validation
    - **Property 9: Image upload type validation**
    - **Validates: Requirements 5.1, 5.2**

  - [x] 2.3 Write property test for unique filename generation
    - **Property 10: Unique filename generation**
    - **Validates: Requirements 5.4**

  - [x] 2.4 Extend EventRepository with dynamic filter queries and category listing
    - Add `findAllWithFilters(EventStatus status, String category, String keyword, Pageable pageable)` using Spring Data JPA Specifications or @Query
    - Add `findDistinctCategoriesByStatus(EventStatus status)` for published-only categories
    - _Requirements: 2.1, 6.1, 6.2, 7.1, 7.2_

  - [x] 2.5 Extend EventService with admin listing, combined search, and CRUD operations
    - Add `getAllEventsAdmin(Pageable, EventStatus, String category, String search)` returning all statuses
    - Add `searchEventsWithFilters(String keyword, String category, LocalDateTime from, LocalDateTime to, Pageable)` with conjunctive filtering
    - Add `createEvent(EventRequest)`, `updateEvent(Long id, EventRequest)`, `updateStatus(Long id, StatusRequest)`, `deleteEvent(Long id)`
    - Ensure new events always get status DRAFT on creation
    - _Requirements: 1.1, 2.1, 3.1, 3.3, 3.4, 4.1, 6.1, 7.1, 7.2, 7.4_

- [x] 3. Backend API: AdminEventController
  - [x] 3.1 Create AdminEventController with all CRUD and image upload endpoints
    - Create `AdminEventController.java` in `com.raincheque.controller`
    - Implement POST /api/v1/admin/events (create, return 201)
    - Implement GET /api/v1/admin/events (list with page, size, status, category, search params)
    - Implement GET /api/v1/admin/events/{id} (get single event)
    - Implement PUT /api/v1/admin/events/{id} (update)
    - Implement PATCH /api/v1/admin/events/{id}/status (status change)
    - Implement DELETE /api/v1/admin/events/{id} (return 204)
    - Implement POST /api/v1/admin/events/{id}/images (image upload with target param)
    - Wire @Valid on request body, return proper HTTP status codes
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 3.1, 3.3, 3.4, 4.1, 4.2, 5.1, 5.5, 8.1, 8.2, 8.3_

  - [x] 3.2 Write property test for event creation always producing DRAFT status
    - **Property 1: Event creation always produces DRAFT status**
    - **Validates: Requirements 1.1**

  - [x] 3.3 Write property test for validation rejecting invalid requests with all errors collected
    - **Property 2: Validation rejects invalid requests with all errors collected**
    - **Validates: Requirements 1.2, 3.2, 9.6**

  - [x] 3.4 Write property test for time range validation
    - **Property 3: Time range validation**
    - **Validates: Requirements 1.5, 9.5**

  - [x] 3.5 Write property test for field constraint enforcement
    - **Property 4: Field constraint enforcement**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.7**

- [x] 4. Checkpoint - Backend compilation and tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Backend: Search, categories, and pagination properties
  - [x] 5.1 Add public categories endpoint and update existing EventController for category filtering
    - Add GET /api/v1/events/categories endpoint returning distinct categories from published events
    - Add optional `category` query parameter to existing GET /api/v1/events endpoint
    - _Requirements: 6.1, 6.2_

  - [x] 5.2 Write property test for category listing reflecting published events only
    - **Property 11: Category listing reflects published events only**
    - **Validates: Requirements 6.1**

  - [x] 5.3 Write property test for case-insensitive search across fields
    - **Property 13: Case-insensitive search across fields**
    - **Validates: Requirements 7.1, 7.4**

  - [x] 5.4 Write property test for combined filters conjunctively applied
    - **Property 14: Combined filters conjunctively applied**
    - **Validates: Requirements 7.2**

  - [x] 5.5 Write property test for pagination metadata consistency
    - **Property 15: Pagination metadata consistency**
    - **Validates: Requirements 8.2**

- [x] 6. Frontend: API service and shared components
  - [x] 6.1 Add adminEventService to api.js
    - Add `adminEventService` object with list, get, create, update, updateStatus, delete, and uploadImage methods
    - All methods use authenticated requests via existing Axios instance
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 5.5_

  - [x] 6.2 Create AdminRoute component
    - Create `AdminRoute.jsx` in `src/components/` wrapping ProtectedRoute with VENUE_ADMIN/ADMIN role check
    - Redirect unauthorized roles to dashboard or show forbidden message
    - _Requirements: 1.4, 4.4_

  - [x] 6.3 Create shared UI components: ImageUploader, PaginationControls, EventFilters
    - Create `ImageUploader.jsx` with drag-and-drop, client-side type/size validation, and preview
    - Create `PaginationControls.jsx` reflecting current page state with navigation buttons
    - Create `EventFilters.jsx` with search input, category dropdown, status dropdown
    - _Requirements: 5.1, 5.2, 5.3, 8.1, 10.3, 10.5, 10.6_

- [x] 7. Frontend: Admin pages and routing
  - [x] 7.1 Create EventManagementPage with responsive list, filters, and pagination
    - Create `EventManagementPage.jsx` in `src/pages/`
    - Display events in responsive table (desktop ≥1024px) / card layout (smaller)
    - Integrate EventFilters and PaginationControls
    - Include action buttons for edit, delete, status change
    - _Requirements: 2.1, 10.4, 10.5, 10.6_

  - [x] 7.2 Create EventFormPage for create and edit with image upload and inline validation
    - Create `EventFormPage.jsx` in `src/pages/`
    - Multi-field form: title, description, short description, venue select, start/end time, category, featured, metadata
    - Integrate ImageUploader for banner and thumbnail
    - Display inline validation errors beneath each field
    - Disable submit button and show loading indicator during save
    - Stack fields vertically at <768px viewport
    - Pre-fill fields when editing (load event by ID from route param)
    - _Requirements: 10.1, 10.2, 10.3, 10.7, 10.8_

  - [x] 7.3 Register admin routes in App.jsx with lazy loading
    - Lazy-load EventManagementPage and EventFormPage
    - Add routes: `/admin/events`, `/admin/events/new`, `/admin/events/:id/edit`
    - Wrap with AdminRoute component
    - _Requirements: 10.1, 10.4_

  - [x] 7.4 Write unit tests for EventFormPage, EventManagementPage, ImageUploader, and AdminRoute
    - Test form field rendering and inline validation display
    - Test list rendering, filter controls, pagination
    - Test image type/size validation and preview
    - Test role-based access control
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_

- [x] 8. Backend integration tests
  - [x] 8.1 Write integration tests for AdminEventController security and CRUD flow
    - Test 401 for unauthenticated requests
    - Test 403 for USER role requests
    - Test full CRUD flow (create → read → update → delete) with H2
    - Test image upload/serve round-trip
    - Test pagination edge cases (empty results, beyond-range page)
    - _Requirements: 1.3, 1.4, 2.1, 2.3, 3.1, 4.1, 4.2, 4.3, 4.4, 5.1, 8.3_

  - [x] 8.2 Write property tests for update round-trip and delete removal
    - **Property 7: Update round-trip preserves data**
    - **Property 8: Delete removes event from existence**
    - **Validates: Requirements 3.1, 4.1**

  - [x] 8.3 Write property test for event visibility matching PUBLISHED status
    - **Property 5: Event visibility matches PUBLISHED status**
    - **Validates: Requirements 3.3, 3.4**

  - [x] 8.4 Write property test for admin listing returning all events regardless of status
    - **Property 6: Admin listing returns all events regardless of status**
    - **Validates: Requirements 2.1**

  - [x] 8.5 Write property test for category filter returning only matching published events
    - **Property 12: Category filter returns only matching published events**
    - **Validates: Requirements 6.2**

- [x] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties using jqwik (backend) and Vitest (frontend)
- Unit tests validate specific examples and edge cases
- The existing `EventController` and `EventResponse` are preserved and reused
- Backend uses Java 17 / Spring Boot 3.5 with Jakarta Validation
- Frontend uses React 18 / Vite 7 / Tailwind CSS 3 with Vitest for testing

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.4", "6.1", "6.2"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.5", "6.3"] },
    { "id": 3, "tasks": ["3.1", "5.1", "7.1", "7.2"] },
    { "id": 4, "tasks": ["3.2", "3.3", "3.4", "3.5", "5.2", "5.3", "5.4", "5.5", "7.3"] },
    { "id": 5, "tasks": ["7.4", "8.1", "8.2", "8.3", "8.4", "8.5"] }
  ]
}
```
