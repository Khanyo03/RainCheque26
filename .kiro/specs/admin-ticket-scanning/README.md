# Rain Cheque — Admin Ticket Scanning & Check-In

Kiro specification for an admin-only QR ticket scanning feature.

## Scope
Only authorised Event Admins can access the scanner and check customers in. Customers may display their QR tickets but cannot access scanning or check-in functionality.

## Files
- `requirements.md`: user stories, requirements, security and acceptance criteria.
- `design.md`: architecture, API contracts, validation and check-in flow.
- `tasks.md`: phased implementation checklist.
- `testing.md`: test scenarios and release verification.

## Kiro workflow
1. Copy `.kiro/specs/admin-ticket-scanning/` into the root of the existing Rain Cheque repository.
2. Ask Kiro to inspect the existing ticket, booking, QR, authentication and event-admin implementation.
3. Review the design against the existing project before coding.
4. Implement one phase at a time from `tasks.md`.
5. Run all tests and verify existing ticket purchase and customer ticket display flows.

## Non-negotiable rules
- Use existing React JavaScript/JSX; do not introduce TypeScript or TSX.
- Use Java 17 and the existing compatible Spring Boot version.
- Reuse existing ticket entities, QR generation, booking relationships, JWT authentication and roles.
- Do not duplicate ticket records or create a second ticketing workflow.
- The backend is the source of truth for validation and check-in.
