# Design — Admin Ticket Scanning & Check-In

## 1. Architecture principles
- Extend the existing Rain Cheque ticket domain; do not create a parallel ticketing system.
- The existing Ticket/Booking service or module remains authoritative for ticket validity and status.
- If Rain Cheque is currently a modular monolith, implement a separated feature/module rather than extracting a microservice just for scanning.
- If ticketing is already split into services, put validation and check-in in the service that owns tickets.
- React handles camera capture and admin UI. The backend makes the final decision.
- Reuse existing JWT, Event Admin roles, event assignments, QR conventions and API response standards.

## 2. User flow
1. Event Admin signs in.
2. Admin opens the Admin Dashboard.
3. Admin selects an event they are authorised to manage.
4. Admin opens Ticket Scanner.
5. Frontend requests camera permission and starts QR scanning.
6. Scanner decodes the QR payload and submits it over HTTPS.
7. Backend authenticates admin, verifies event access, validates ticket and checks event association/status.
8. If valid and unused, backend atomically records check-in.
9. Frontend displays accepted or rejected result.
10. Admin can scan the next ticket and view attendance totals.

## 3. Suggested frontend structure
Adapt to the existing Rain Cheque layout; reuse current admin routes, layout, API client and components.

```text
src/
├── pages/admin/
│   └── AdminTicketScannerPage.jsx
├── components/admin/ticket-scanner/
│   ├── TicketScanner.jsx
│   ├── ScanResult.jsx
│   ├── ScannerControls.jsx
│   ├── EventSelector.jsx
│   └── AttendanceSummary.jsx
├── services/
│   └── adminTicketService.js
└── hooks/
    └── useQrScanner.js
```

Frontend responsibilities:
- Protect route using existing admin route guard.
- List only events assigned to current admin.
- Start/stop camera lifecycle safely.
- Decode QR and submit opaque payload/reference.
- Prevent repeated in-flight submissions.
- Display accessible outcomes.
- Refresh attendance summary after successful check-in.
- Never decide final ticket validity locally.

Use a maintained QR library compatible with the current React/browser versions. Inspect dependency compatibility before adding it. Use JavaScript and JSX only.

## 4. Backend logical components
Adapt package names to existing conventions:

```text
controller/
  AdminTicketCheckInController.java
service/
  TicketValidationService.java
  TicketCheckInService.java
  EventAdminAuthorisationService.java
repository/
  TicketRepository.java
  TicketCheckInRepository.java
dto/
  TicketScanRequest.java
  TicketScanResponse.java
  AttendanceSummaryResponse.java
```

Do not duplicate existing Ticket, Booking, Event or User entities. Create a check-in entity only if no equivalent persistence exists.

## 5. Suggested API contract
Inspect existing routes first and avoid collisions.

### GET `/api/admin/events/{eventId}/scanner/summary`
Returns event information and attendance counts for an authorised Event Admin.

### POST `/api/admin/events/{eventId}/tickets/scan`
Request:

```json
{
  "qrPayload": "opaque-ticket-token"
}
```

Successful response example:

```json
{
  "result": "VALID_CHECKED_IN",
  "message": "Ticket accepted. Customer checked in.",
  "ticketReference": "RC-8F3A21",
  "eventId": "event-uuid",
  "checkedInAt": "2026-11-10T13:30:00Z",
  "ticketType": "General Admission"
}
```

Already checked in:

```json
{
  "result": "ALREADY_CHECKED_IN",
  "message": "This ticket has already been used.",
  "ticketReference": "RC-8F3A21",
  "checkedInAt": "2026-11-10T13:30:00Z"
}
```

### GET `/api/admin/events/{eventId}/check-ins`
Returns paginated recent check-ins for an authorised admin.

Follow existing API response conventions. Avoid leaking private customer data.

## 6. Validation and atomic check-in
Recommended sequence:
1. Authenticate JWT.
2. Authorise Event Admin role.
3. Verify admin may manage `eventId`.
4. Resolve and validate QR token using existing ticket QR rules.
5. Load ticket and associated booking/event.
6. Confirm ticket belongs to requested event.
7. Confirm payment/booking/ticket status is eligible.
8. Atomically claim ticket for check-in.
9. Record server-generated UTC timestamp and admin user ID.
10. Return result DTO.

A read-then-write check alone is unsafe because two scanners can read "not checked in" concurrently. Use a transaction and an atomic conditional update, row lock, or unique constraint.

Example only; adapt ID types and foreign keys to the existing entities:

```sql
CREATE TABLE ticket_check_ins (
    id UUID PRIMARY KEY,
    ticket_id BIGINT NOT NULL UNIQUE,
    event_id UUID NOT NULL,
    checked_in_by BIGINT NOT NULL,
    checked_in_at TIMESTAMPTZ NOT NULL
);
```

If the current Ticket table already has a reliable check-in state, use it with suitable constraints rather than duplicating state. Translate duplicate constraint conflicts into `ALREADY_CHECKED_IN`, not HTTP 500.

## 7. QR payload security
- Reuse the current QR payload if it is secure and suitable.
- Prefer opaque, high-entropy references or cryptographically signed tokens.
- Do not encode names, email addresses, phone numbers or payment details.
- A decoded sequential database ID is not proof of authenticity.
- If current QR codes contain predictable IDs, plan a backward-compatible secure token change before production.
- Never log raw QR payloads.

## 8. Security
- Reuse current Spring Security filter chain and JWT validation.
- Reuse existing Event Admin authority naming.
- Verify event-level admin assignment for every scan, summary and history request.
- Frontend protection is UX only; backend enforcement is mandatory.
- Return 401 unauthenticated and 403 unauthorised.
- Configure explicit trusted CORS origins.
- Use HTTPS in deployed environments.

## 9. Attendance
Calculate from authoritative backend records:
- `totalEligibleTickets`
- `checkedInCount`
- `notCheckedInCount`
- optionally `checkInPercentage`

Apply existing business rules to determine eligible tickets. Exclude cancelled/refunded tickets unless the current domain rules explicitly say otherwise.

## 10. Operational considerations
- Stop camera tracks on unmount/navigation.
- Require explicit user action before camera activation.
- Debounce detections on the client; enforce idempotency on the backend.
- Avoid resubmitting a QR while its request is in flight.
- Add correlation IDs and safe audit logs.
- Do not store camera images or video.
- Define scan-attempt log retention according to business/legal requirements.
