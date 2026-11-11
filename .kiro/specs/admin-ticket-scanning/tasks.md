# Kiro Tasks — Admin Ticket Scanning & Check-In

Implement one phase at a time. Inspect the existing project before modifying code.

## Phase 0 — Repository discovery
- [ ] Inspect frontend admin routes and route guards.
- [ ] Inspect backend User roles/authorities and JWT security.
- [ ] Inspect Event, Ticket and Booking entities and repositories.
- [ ] Inspect ticket QR generation and exact QR payload currently issued.
- [ ] Inspect payment/refund/cancellation states and ticket eligibility rules.
- [ ] Inspect any existing check-in field or check-in entity.
- [ ] Inspect event-admin assignment/ownership rules.
- [ ] Inspect API response and exception conventions.
- [ ] Run baseline frontend and backend tests.
- [ ] Produce a reuse/change report before coding.

Acceptance: Kiro identifies the authoritative ticket model, QR format, admin permission and event ownership checks. No duplicate entities/endpoints are created prematurely.

## Phase 1 — Backend authorisation and API contract
- [ ] Confirm existing Event Admin role/authority.
- [ ] Define endpoint contract and result codes.
- [ ] Implement backend role enforcement.
- [ ] Implement event-level admin authorisation.
- [ ] Test that customers and unrelated admins cannot scan.
- [ ] Keep existing public/customer APIs unchanged.

Acceptance: direct customer API calls return 403; admins can access only events they may manage.

## Phase 2 — Ticket validation and atomic check-in
- [ ] Reuse existing QR token verification or implement a secure opaque/signed token strategy.
- [ ] Validate ticket existence and authenticity.
- [ ] Validate event association.
- [ ] Validate payment, cancellation, refund and ticket status using existing rules.
- [ ] Implement atomic check-in using a transaction and database constraint/conditional update.
- [ ] Record server timestamp and admin identity.
- [ ] Map expected rejections to documented result codes.
- [ ] Add a migration only if current schema lacks required persistence.
- [ ] Add unit and database integration tests.

Acceptance: valid tickets check in once; invalid/ineligible tickets are rejected; concurrent duplicate scans cannot create multiple successful check-ins.

## Phase 3 — Admin scanner frontend
- [ ] Add scanner route inside existing admin route protection.
- [ ] Add event selector limited to events managed by current admin.
- [ ] Add camera scanner using a compatible QR library.
- [ ] Request camera access only after explicit admin action.
- [ ] Implement camera selection where supported.
- [ ] Implement start, pause, resume and stop controls.
- [ ] Stop camera tracks on unmount/navigation.
- [ ] Prevent duplicate in-flight scan submissions.
- [ ] Submit decoded payload to backend.
- [ ] Display clear accepted/rejected outcomes.
- [ ] Add accessible permission-denied and camera-error states.
- [ ] Use JSX/JavaScript only; no TSX.
- [ ] Add frontend tests.

Acceptance: authorised admin can scan a ticket and see the backend result.

## Phase 4 — Attendance dashboard and history
- [ ] Implement attendance summary endpoint.
- [ ] Implement paginated recent check-in endpoint.
- [ ] Add checked-in, remaining and eligible ticket counts.
- [ ] Refresh counts after successful scan.
- [ ] Restrict summary/history to authorised event admins.
- [ ] Test count correctness and event-level access.

Acceptance: admins see accurate attendance data only for authorised events.

## Phase 5 — Security, resilience and usability
- [ ] Verify customer route access is blocked.
- [ ] Verify customer direct API access is blocked.
- [ ] Verify admin access to another organiser's event is blocked.
- [ ] Verify wrong-event QR rejection.
- [ ] Verify duplicate and concurrent scan behaviour.
- [ ] Verify camera unavailable/denied states.
- [ ] Verify raw QR payload and JWT are not logged.
- [ ] Verify mobile and desktop browser behaviour.
- [ ] Review rate limits and safe error responses.

Acceptance: all security and failure scenarios pass.

## Phase 6 — Regression and deployment
- [ ] Run backend unit and integration tests.
- [ ] Run frontend tests and production build.
- [ ] Run existing booking, payment, ticket issuance and customer ticket-display tests.
- [ ] Verify existing QR codes still work.
- [ ] Update API documentation.
- [ ] Document camera/browser requirements and admin usage.
- [ ] Configure production HTTPS and environment variables.
- [ ] Deploy to test environment and perform an end-to-end entry rehearsal.

Acceptance: scanner is ready for the agreed MVP scope and existing Rain Cheque flows remain intact.

## Definition of Done
- Only authorised Event Admins can access scanner functionality.
- Backend validates scans and performs atomic check-in.
- Duplicate scans are safely rejected.
- Attendance data is accurate and event-scoped.
- Existing ticket issuance and customer QR display continue working.
- Automated tests pass.
- No secrets or raw QR tokens are committed or logged.
- No TypeScript/TSX is introduced.
