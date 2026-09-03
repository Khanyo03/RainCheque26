# Rain Cheque

Rain Cheque is a full-stack event ticketing platform. Users browse and search published events, purchase tickets through a pluggable payment provider, and receive QR-coded PDF tickets by email. Venue administrators manage events, ticket tiers, and bulk ticket generation, while staff validate tickets at the door.

## Tech Stack

**Backend**
- Java 17, Spring Boot 3.5.14
- Spring Web, Spring Data JPA, Spring Security, Spring Validation, Spring Actuator
- PostgreSQL (production), H2 (development and tests)
- Redis (rate limiting / caching support)
- JWT authentication (jjwt 0.13)
- ZXing (QR code generation), iText 7 (PDF tickets), Thymeleaf (PDF templates)
- Spring Mail (ticket delivery)
- Maven build, JUnit 5 + Spring Security Test + jqwik (property-based testing)

**Frontend**
- React 18, Vite 7
- React Router v6 (with route-based code splitting)
- Tailwind CSS, axios, date-fns, lucide-react
- Vitest + Testing Library + jsdom

**Infrastructure**
- Docker Compose (PostgreSQL, Redis, backend, frontend, pgAdmin)
- Kubernetes manifests (namespace, config, secrets, deployments, ingress)
- Nginx (frontend container)

## Repository Layout

```
RainCheque26/
├── backend-spring/rain-cheque-backend/   Spring Boot backend
├── frontend/RAIN-CHEQUE/                 React + Vite frontend
├── database/                             SQL schema and seed scripts
├── docker/                               docker-compose.yml
├── k8s/                                  Kubernetes manifests
└── docs/                                 Project documentation
```

See ARCHITECTURE.md for a detailed breakdown of the system design.

## Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- Node.js 18+ and npm
- Docker and Docker Compose (optional, for containerized run)

## Environment Variables

The backend reads configuration from environment variables. Copy the template and fill in values:

```
cp .env.example .env
```

Required variables (see `.env.example` for the full list):

| Variable | Purpose |
|----------|---------|
| `SPRING_SECURITY_JWT_SECRET` | Secret used to sign and verify JWT tokens |
| `APP_QR_SECRET` | Secret used to encrypt/decrypt QR code payloads |
| `PAYSTACK_API_KEY` | Paystack secret API key |
| `PAYSTACK_WEBHOOK_SECRET` | Secret used to verify Paystack webhook signatures |
| `SPRING_DATASOURCE_URL` | JDBC URL (production / PostgreSQL profile) |
| `SPRING_DATASOURCE_USERNAME` | Database username (production) |
| `SPRING_DATASOURCE_PASSWORD` | Database password (production) |
| `POSTGRES_PASSWORD` | PostgreSQL password (docker-compose) |

> Note: Spring Boot does not read `.env` files natively. Running through the VS Code launch configuration loads `.env` automatically. When running via Maven directly, export the variables in your shell first (see below).

## Running Locally

### Backend

The `dev` profile uses an in-memory H2 database that is created and seeded on startup, so no external database is required.

Using Maven (export environment variables first):

```
cd backend-spring/rain-cheque-backend
# set the environment variables from your .env in the current shell, then:
mvn spring-boot:run
```

The API starts on `http://localhost:8081`.

Health check: `http://localhost:8081/actuator/health`
H2 console (dev only): `http://localhost:8081/h2-console`

### Frontend

```
cd frontend/RAIN-CHEQUE
npm install
npm run dev
```

The dev server starts on `http://localhost:5173` and proxies API calls to the backend.

## Running with Docker Compose

From the `docker/` directory, with a populated `.env` at the repository root:

```
cd docker
docker compose up --build
```

Services:

| Service | Port |
|---------|------|
| Frontend (nginx) | 80 |
| Backend API | 8081 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| pgAdmin | 5050 |

The backend container runs with the `prod` profile against PostgreSQL.

## Testing

Backend:

```
cd backend-spring/rain-cheque-backend
mvn test
```

Frontend:

```
cd frontend/RAIN-CHEQUE
npm test
```

## API Overview

All endpoints are prefixed with `/api/v1`. A representative selection:

| Method | Path | Access |
|--------|------|--------|
| POST | `/auth/register` | Public |
| POST | `/auth/login` | Public |
| GET | `/events` | Public |
| GET | `/events/search` | Public |
| GET | `/events/{id}` | Public |
| POST | `/admin/events` | ADMIN, VENUE_ADMIN |
| PUT | `/admin/events/{id}` | ADMIN, VENUE_ADMIN |
| PATCH | `/admin/events/{id}/status` | ADMIN, VENUE_ADMIN |
| POST | `/admin/events/{id}/images` | ADMIN, VENUE_ADMIN |
| GET | `/tickets` | Authenticated |
| GET | `/tickets/{id}/download` | Authenticated |
| POST | `/tickets/validate` | Authenticated |
| POST | `/tickets/batch/event/{eventId}` | Authenticated |
| POST | `/payments/checkout` | Authenticated |
| POST | `/payments/{id}/refund` | ADMIN |
| POST | `/payments/webhook/{provider}` | Public (signature-verified) |

## License

© 2026 Rain Cheque, developed by Khanyo Railo. All rights reserved.
