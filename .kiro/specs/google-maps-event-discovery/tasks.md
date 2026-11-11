# Implementation Tasks — Google Maps Live Event Discovery

Build in small, testable increments. Before editing, inspect the existing Rain Cheque repositories and report which services, entities, endpoints, authentication, Kafka setup, and frontend components already exist. Reuse them; do not scaffold duplicates.

## Phase 0 — Repository discovery and baseline
- [ ] Inspect frontend and backend repositories.
- [ ] Identify the existing Event entity, event lifecycle, event status values, event details route, authentication/JWT implementation, API gateway, and database.
- [ ] Identify current Spring Boot, Spring Cloud, Java, React, and Node versions.
- [ ] Identify existing Kafka infrastructure and conventions.
- [ ] Record proposed files to create/change before implementation.
- [ ] Confirm no TypeScript/TSX is introduced.
- [ ] Run baseline frontend and backend tests.

Acceptance: a short repository assessment identifies reuse points and any blockers before code changes.

## Phase 1 — Data contract and event publishing
- [ ] Define versioned lifecycle event envelope.
- [ ] Confirm stable event ID and version semantics.
- [ ] Implement or reuse transactional outbox in Event Service.
- [ ] Publish `EVENT_PUBLISHED`, `EVENT_UPDATED`, and `EVENT_CANCELLED`.
- [ ] Add schema validation and producer tests.
- [ ] Document Kafka topic, key, retention, retry, and dead-letter strategy.

Acceptance: event lifecycle changes produce reliable, versioned Kafka messages without breaking existing event management.

## Phase 2 — Location Service and PostGIS
- [ ] Create Location Service using Java 17 and compatible Spring Boot.
- [ ] Configure PostgreSQL with PostGIS.
- [ ] Add Flyway migration for event location projection and GiST index.
- [ ] Implement Kafka consumer with idempotent upsert/delete.
- [ ] Reject stale event versions.
- [ ] Implement nearby query repository using ST_DWithin.
- [ ] Implement map bounds query.
- [ ] Validate coordinates, radius, dates, bounds, and result limits.
- [ ] Add unit and Testcontainers integration tests.

Acceptance: published events are searchable by radius and map bounds; updates and cancellations correctly update the projection.

## Phase 3 — Discovery API
- [ ] Create or extend Discovery Service.
- [ ] Implement `GET /api/discovery/events`.
- [ ] Implement `GET /api/discovery/events/nearby`.
- [ ] Implement `GET /api/discovery/events/{eventId}`.
- [ ] Add category/date filters and bounded result limits.
- [ ] Add OpenAPI documentation and standard error responses.
- [ ] Route endpoints through API Gateway.
- [ ] Add security and contract tests.

Acceptance: frontend can retrieve map summaries using documented, secured or intentionally public endpoints.

## Phase 4 — React Google Maps interface
- [ ] Configure Google Maps browser key using environment configuration.
- [ ] Add Event Discovery page and route using existing routing conventions.
- [ ] Render map with configured default centre.
- [ ] Fetch initial map-bounds snapshot.
- [ ] Display event markers and marker clustering.
- [ ] Display event info window/card.
- [ ] Link to existing event details page.
- [ ] Add Places search if enabled.
- [ ] Add category and date filters.
- [ ] Debounce viewport changes and cancel stale requests.
- [ ] Add loading, empty, error, and permission states.
- [ ] Add frontend component and service tests.

Acceptance: user can explore the map, search and filter events, select a marker, and open event details.

## Phase 5 — Real-time updates
- [ ] Create or extend Real-Time Service.
- [ ] Consume lifecycle messages from Kafka.
- [ ] Expose authenticated/authorised WebSocket endpoint as appropriate.
- [ ] Broadcast minimal event update messages.
- [ ] Implement frontend socket connection and subscription.
- [ ] Reconcile marker changes by event ID and version.
- [ ] Implement reconnect and snapshot refetch.
- [ ] Add tests for publish, update, cancellation, duplicate, stale, disconnect, and reconnect cases.

Acceptance: a published event appears, an updated event changes, and a cancelled event disappears without manual page refresh; reconnect restores a consistent snapshot.

## Phase 6 — Gateway, security, and hardening
- [ ] Configure REST and WebSocket routing.
- [ ] Validate JWTs and enforce existing authorisation rules.
- [ ] Configure explicit CORS origins.
- [ ] Restrict Google Maps browser key by referrer and API.
- [ ] Add request rate limits and maximum spatial query bounds.
- [ ] Add correlation IDs, structured logs, health checks, and metrics.
- [ ] Review public event DTOs for privacy leaks.

Acceptance: security review passes and invalid or abusive map queries are rejected.

## Phase 7 — Local deployment and documentation
- [ ] Add or update Docker Compose for Kafka and PostgreSQL/PostGIS.
- [ ] Document environment variables without committing secrets.
- [ ] Add database migration and startup instructions.
- [ ] Add service API and event-contract documentation.
- [ ] Add manual end-to-end test checklist.
- [ ] Run all backend, frontend, integration, and regression tests.
- [ ] Verify existing Rain Cheque login, event management, event details, and booking flows.

Acceptance: a developer can run the feature locally from documented instructions and existing Rain Cheque functionality remains intact.

## Definition of Done
- All acceptance criteria in requirements.md pass.
- Automated tests cover critical paths and failure modes.
- No TypeScript/TSX has been added.
- No secrets are committed.
- Existing APIs and user flows are not broken.
- API and local setup documentation are updated.
- No unrelated refactoring is included.
