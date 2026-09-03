# Rain Cheque — Architecture

This document describes the technical design of the Rain Cheque event ticketing platform: its major components, domain model, request flows, and cross-cutting concerns.

## System Overview

Rain Cheque is a two-tier web application with a React single-page frontend and a Spring Boot REST backend, backed by a relational database and Redis. Payments are handled through a provider-agnostic abstraction that currently supports Stripe and Paystack.

```
┌─────────────────┐     HTTPS/JSON      ┌──────────────────────┐
│  React SPA      │ ──────────────────> │  Spring Boot API     │
│  (Vite, nginx)  │ <────────────────── │  (REST, port 8081)   │
└─────────────────┘                     └──────────┬───────────┘
                                                    │
                        ┌───────────────────────────┼───────────────────────────┐
                        │                           │                           │
                  ┌─────▼─────┐              ┌──────▼──────┐            ┌────────▼────────┐
                  │ PostgreSQL│              │    Redis    │            │ Payment Provider │
                  │  (JPA)    │              │ (rate limit)│            │ (Stripe/Paystack)│
                  └───────────┘              └─────────────┘            └─────────────────┘
```

## Backend Architecture

The backend follows a conventional layered architecture:

```
Controller  ->  Service  ->  Repository  ->  Entity (JPA)
   (REST)      (business)     (Spring Data)    (domain)
```

### Packages

| Package | Responsibility |
|---------|----------------|
| `controller` | REST endpoints (events, auth, admin events, tickets, test) |
| `service` | Business logic (events, tickets, users, email, PDF, QR, batch, images) |
| `repository` | Spring Data JPA repositories |
| `entity` | JPA domain entities and enums |
| `dto` | Request/response data transfer objects |
| `security` | JWT filter, rate-limit filter, user details service |
| `config` | Security configuration and application wiring |
| `validation` | Custom bean validation constraints |
| `exception` | Exception types and global handling |
| `payment` | Payment abstraction, providers, controllers, and configuration |

### Domain Model

```
Venue 1───* Event 1───* TicketTier 1───* Booking 1───* Ticket
                                              │
                                              *
                                              │
                                          Payment
User 1───* Booking
Event 1───* TicketBatchJob
```

- **User** — account with a role (`USER`, `VENUE_ADMIN`, `ADMIN`); BCrypt-hashed password.
- **Venue** — physical location hosting events; has capacity and contact metadata.
- **Event** — an event at a venue with a lifecycle status (`DRAFT`, `PUBLISHED`, `CANCELLED`, `COMPLETED`). Holds ticket tiers and a JSON metadata blob (age restriction, weather policy, parking, food).
- **TicketTier** — a priced category of tickets for an event with total/remaining quantity and per-user limits.
- **Booking** — a user's purchase of a quantity from a tier, with a status (`PENDING`, `CONFIRMED`, `CANCELLED`, `FAILED`, `REFUNDED`).
- **Ticket** — an individual issued ticket with a unique code, encrypted QR secret, and status (`ACTIVE`, `USED`, `CANCELLED`, `EXPIRED`).
- **Payment** — a payment against a booking, tracking provider, provider references, amount, and status (`PENDING`, `PROCESSING`, `SUCCESS`, `FAILED`, `CANCELLED`, `REFUNDED`, `EXPIRED`).
- **TicketBatchJob** — tracks asynchronous bulk ticket generation for an event.

### Key Services

- **EventService** — event CRUD, publishing, search, category listing, visibility rules.
- **TicketService** — ticket retrieval, download, and validation.
- **BatchTicketService / BookingTicketGenerator** — asynchronous bulk ticket generation, tracked via `TicketBatchJob`.
- **QrCodeService** — generates encrypted QR payloads using ZXing and `APP_QR_SECRET`.
- **PdfTicketService** — renders PDF tickets with iText 7 and Thymeleaf templates.
- **EmailService** — sends tickets and notifications via Spring Mail (`JavaMailSender`).
- **ImageUploadService** — handles event banner/thumbnail uploads.
- **UserService** — user lookup and management.

## Security

Authentication is stateless and token-based.

- **Login/registration** issue a signed JWT (jjwt, `SPRING_SECURITY_JWT_SECRET`).
- **JwtAuthenticationFilter** validates the bearer token on each request and populates the security context.
- **RateLimitFilter** runs ahead of authentication to throttle requests.
- **CustomUserDetailsService** loads users for the `DaoAuthenticationProvider`; passwords are hashed with BCrypt.
- Sessions are `STATELESS`; CSRF is disabled (token-based API); CORS is restricted to configured origins.

### Authorization Rules

| Path pattern | Access |
|--------------|--------|
| `/api/v1/auth/**` | Public |
| `/api/v1/events/**` | Public |
| `/uploads/**` | Public |
| `/actuator/health/**` | Public |
| `/api/v1/payments/webhook/**` | Public (signature-verified) |
| `/api/v1/payments/*/refund` | `ADMIN` |
| `/api/v1/payments/**` | Authenticated |
| `/api/v1/admin/**` | `VENUE_ADMIN`, `ADMIN` |
| everything else | Authenticated |

Roles are `USER`, `VENUE_ADMIN`, and `ADMIN`.

## Payment Abstraction

Payments are decoupled from any single vendor through a provider interface.

```
PaymentController
      │
      ▼
PaymentService  ──>  PaymentProviderFactory  ──>  PaymentProvider (interface)
                                                     ├── StripePaymentProvider
                                                     └── PaystackPaymentProvider
```

- **PaymentProvider** defines the contract: `createCheckoutSession`, `verifyPayment`, `refundPayment`, `cancelPayment`, `retrievePayment`, `getPaymentHistory`, `constructWebhookEvent`, `getProviderType`.
- **PaymentProviderFactory** discovers all provider beans at startup and registers only those enabled in configuration, keyed by `PaymentProviderType` (`STRIPE`, `PAYSTACK`, `PAYFAST`, `YOCO`, `PEACH`, `OZOW`).
- Each provider is registered conditionally via `@ConditionalOnProperty(payment.providers.<TYPE>.enabled=true)`.
- **PaymentProperties** binds per-provider configuration (enabled flag, API key, webhook secret) and validates that enabled providers have the required credentials at startup.

### Provider Selection

`payment.default-provider` chooses the active provider. Per-provider blocks under `payment.providers` control enablement and credentials, which are sourced from environment variables. In the `dev` profile, Paystack is enabled and set as the default while other providers are disabled.

### Webhooks

`POST /api/v1/payments/webhook/{provider}` is public but signature-verified. The controller resolves the provider-specific signature header (for example, `Stripe-Signature` or `x-paystack-signature`), and the provider verifies it before the event is processed. Paystack signatures are verified using HMAC SHA-512 over the raw payload.

## Data Layer and Profiles

- **dev** — in-memory H2 (`create-drop`), seeded from `data.sql` on startup; H2 console enabled. Mail and Redis health indicators are disabled for local convenience.
- **prod** — PostgreSQL via `SPRING_DATASOURCE_*`; used by the Docker/Kubernetes deployments.
- Flyway is present as a dependency but disabled; schema is managed by Hibernate DDL in dev and by the SQL scripts in `database/` for containerized runs.
- Some columns (event metadata, venue metadata, booking seat numbers, ticket tier benefits) use native JSON storage.

## Frontend Architecture

The frontend is a React SPA built with Vite.

- **Routing** — React Router v6. Heavier pages are lazy-loaded via `React.lazy` and `Suspense` for route-based code splitting.
- **Route protection** — `ProtectedRoute` guards authenticated pages; `AdminRoute` guards administrator pages.
- **State** — authentication state is provided through a React context.
- **API access** — a single axios-based client (`services/api.js`) centralizes HTTP calls and attaches the JWT.
- **Error handling** — a top-level `ErrorBoundary` wraps the routed content.
- **Styling** — Tailwind CSS.

### Pages

Home, Events, Event Detail, Login, Register, Dashboard, Tickets, Profile, Admin Event Management, Admin Event Form, and a Not Found fallback.

### Notable Components

`Navbar`, `EventCard`, `EventFilters`, `TicketCard`, `ImageUploader`, `PaginationControls`, `LoadingSpinner`, `ErrorAlert`, plus the `ProtectedRoute`/`AdminRoute` guards.

## Request Flow: Ticket Purchase (illustrative)

1. A user browses published events (`GET /api/v1/events`) and opens an event detail page.
2. The user initiates checkout; the frontend calls `POST /api/v1/payments/checkout` with the booking details.
3. `PaymentService` resolves the active provider through `PaymentProviderFactory` and creates a checkout session; the provider returns a hosted checkout URL.
4. The user completes payment on the provider's page.
5. The provider calls back to `POST /api/v1/payments/webhook/{provider}`; the signature is verified and the payment/booking status is updated.
6. Tickets are generated (QR + PDF) and delivered by email; the user can also download them via `GET /api/v1/tickets/{id}/download`.

## Testing Strategy

- **Backend** — JUnit 5 with Spring Boot Test and Spring Security Test for unit and integration tests, plus **jqwik** property-based tests for invariants such as amount conversions, status mappings, and signature verification.
- **Frontend** — Vitest with Testing Library and jsdom for component and page tests.

## Deployment

- **Docker Compose** provisions PostgreSQL, Redis, the backend (prod profile), the nginx-served frontend, and pgAdmin on a shared bridge network with health checks and dependency ordering.
- **Kubernetes** manifests under `k8s/` define a namespace, config map, secrets, backend and frontend deployments, and an ingress for cluster deployment.

## Cross-Cutting Concerns

- **Configuration** is environment-variable driven; secrets are never hardcoded and are supplied via `.env`, Docker environment, or Kubernetes secrets.
- **Observability** is exposed through Spring Actuator health endpoints, consumed by container/orchestrator health checks.
- **Rate limiting** is enforced by a servlet filter ahead of authentication.
- **Error sanitization** in payment providers strips secret keys and internal identifiers from error messages before they surface.
