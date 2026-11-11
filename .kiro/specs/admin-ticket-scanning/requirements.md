# Requirements — Admin Ticket Scanning & Check-In

## 1. Purpose
Provide secure QR-code scanning and customer check-in for Rain Cheque. Only authorised Event Admins may scan tickets. Customers present their existing QR-coded tickets but cannot access scanner or check-in functionality.

## 2. Actors
- **Event Admin:** authenticated user with the existing Event Admin permission, limited to events they are authorised to manage.
- **Customer:** can view and present their ticket QR code; cannot scan tickets or invoke check-in operations.
- **Backend:** validates ticket authenticity, event association, ticket status, admin permissions and check-in state.

## 3. User stories
- As an Event Admin, I want to open a scanner for an event I manage so I can check customers in.
- As an Event Admin, I want to scan a QR ticket and receive a clear validation result.
- As an Event Admin, I want invalid, cancelled, refunded, wrong-event and already-used tickets rejected.
- As an Event Admin, I want successful check-ins recorded with timestamp and admin identity.
- As an Event Admin, I want to view attendance counts and recent check-ins.
- As a customer, I want to display my ticket QR code without having access to scanning functionality.

## 4. Functional requirements

### FR-1 Admin-only access
- Protect the scanner route using the existing frontend admin route mechanism.
- Enforce authentication and Event Admin authority on every backend scan, validation, summary and history endpoint.
- Verify the admin is authorised to manage the requested event.
- Hiding a button in React is not sufficient security.
- Return 401 for unauthenticated requests and 403 for authenticated users without permission.
- Reuse the existing role model; do not create duplicate roles unnecessarily.

### FR-2 Event selection
- Admins can select only events they are authorised to manage.
- Show the selected event name and date in the scanner.
- A ticket for another event must never be checked in.

### FR-3 QR scanning
- Use a maintained browser-compatible QR scanner library after checking compatibility with the current React version.
- Request camera permission only after the admin explicitly starts scanning.
- Handle permission denied, camera unavailable, unsupported browser and camera-in-use states.
- Allow camera selection where supported.
- Stop camera tracks when scanning stops or the component unmounts.
- Debounce repeated detections on the client, while retaining mandatory backend idempotency.
- A manual ticket-reference fallback is optional and requires product/security approval.

### FR-4 Ticket validation
The backend must validate:
- QR token/reference authenticity using the existing QR design or a secure replacement.
- Ticket existence.
- Ticket-to-event association.
- Existing booking/payment eligibility rules.
- Cancellation/refund status.
- Whether the ticket has already been checked in.
- Whether the admin may manage the event.

Never trust browser-supplied validity, ticket status, event ownership, customer identity or check-in timestamp.

### FR-5 Check-in
- A valid unused ticket is checked in through an authenticated backend operation.
- Record ticket ID, event ID, server-generated UTC timestamp and admin user ID.
- Check-in must be atomic to prevent concurrent scanners accepting the same ticket twice.
- Duplicate scans return `ALREADY_CHECKED_IN` and do not create another check-in.
- Failed validation must not create a successful check-in.

### FR-6 Scan outcomes
Use stable result codes:
- `VALID_CHECKED_IN`
- `ALREADY_CHECKED_IN`
- `INVALID_TICKET`
- `WRONG_EVENT`
- `CANCELLED`
- `REFUNDED`
- `PAYMENT_NOT_CONFIRMED`
- `UNAUTHORISED_EVENT`

Show only the minimum customer information needed by staff, such as permitted display name and ticket type.

### FR-7 Attendance
- Show total eligible tickets, checked-in count and remaining count.
- Provide paginated recent check-ins.
- Calculate counts on the backend.
- Scope all attendance data to events the admin may manage.

### FR-8 Audit
- Record successful check-ins with admin identity and server timestamp.
- Do not log raw QR tokens, JWTs or payment details.
- If rejected attempts are audited, store safe reason codes rather than raw secrets.

### FR-9 Customer experience
- Existing customer ticket display and QR generation remain functional.
- Customers do not see scanner navigation or admin scanner pages.
- Customers cannot perform check-in through direct API calls.

## 5. Security
- Reuse existing Spring Security and JWT configuration.
- Enforce role and event-level access on the backend.
- Use HTTPS outside local development.
- Prefer opaque, high-entropy, non-sequential QR references or cryptographically signed tokens, compatible with the existing QR design.
- Do not encode personal or payment information in QR codes.
- Do not treat a decoded database ID as proof of authenticity.
- Apply appropriate rate limiting.
- Prevent IDOR by checking event-admin assignment on every request.
- Never expose whether arbitrary ticket IDs exist to unauthorised callers.

## 6. Non-functional requirements
- Check-in must be transactionally safe.
- Duplicate and concurrent scans must be handled consistently.
- Support common mobile and desktop browsers with cameras.
- Provide accessible text outcomes; do not rely on colour alone.
- Use UTC timestamps in backend contracts.
- Follow existing API error and response conventions.

## 7. Acceptance criteria
1. An authorised Event Admin can open the scanner for an event they manage.
2. Customers cannot access the scanner page.
3. Customers cannot call scan/check-in APIs successfully.
4. Admins cannot scan tickets for events they do not manage.
5. A valid eligible ticket for the selected event is checked in.
6. A ticket for another event is rejected.
7. Cancelled, refunded or payment-ineligible tickets are rejected according to existing business rules.
8. Duplicate scans return `ALREADY_CHECKED_IN`.
9. Concurrent scans produce at most one successful check-in.
10. The backend records admin identity and timestamp.
11. Attendance counts reflect successful check-ins.
12. Camera permission and failure states are handled.
13. Existing booking, payment, ticket issuance and customer QR display flows continue working.
14. No TypeScript/TSX files are introduced.
