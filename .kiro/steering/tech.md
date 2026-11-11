# Technical Steering — Live Event Discovery

## Mandatory technology choices
- Frontend: existing React application using JavaScript and `.jsx`; do not introduce TypeScript or `.tsx`.
- Backend: Java 17 and Spring Boot 3.x.
- API gateway: Spring Cloud Gateway, on a release train compatible with the selected Spring Boot version.
- Persistence: PostgreSQL.
- Geospatial search: PostGIS using SRID 4326 and geography distance calculations.
- Messaging: Apache Kafka.
- Live browser updates: Spring WebSocket with STOMP (or a documented plain WebSocket alternative).
- Frontend HTTP: existing Axios setup where available.
- Containers: Docker / Docker Compose for local development.
- Tests: JUnit 5, Mockito, Spring Boot Test, Testcontainers, and frontend test tooling already used by the repository.

## Architecture rules
- Event Service owns event lifecycle and authoritative event data.
- Location Service owns the read-optimised geographical projection of published events.
- Discovery Service exposes map-oriented query APIs and reads the Location Service projection through a documented internal API or shared query contract. Do not allow services to write directly to another service's tables.
- Real-Time Service consumes domain events and broadcasts minimal update notifications to subscribed clients.
- Kafka messages are versioned, contain stable event IDs, and are idempotently processed.
- Use the transactional outbox pattern in Event Service if feasible; do not rely on a database write and Kafka publish being atomic without an outbox or equivalent reliability strategy.
- Use bounded map searches and enforce maximum radius, bounding-box size, page size, and request rate.
- Do not broadcast every event globally when a map viewport or geographic subscription can be used.
- The frontend must fetch an initial snapshot over HTTP, then subscribe to live updates. Reconcile updates by event ID and event version.
- Handle reconnects by re-fetching the current viewport snapshot; do not assume WebSocket delivery is durable.

## Security
- Validate JWTs at the gateway and enforce authorisation in services for protected operations.
- Public discovery endpoints expose only public event fields.
- Keep Google Maps browser key separate from server-side credentials.
- Restrict browser key by allowed HTTP referrers and API restrictions.
- Never commit secrets. Use environment variables or a secrets manager.
- Configure CORS for explicit frontend origins.
- Validate all coordinates, radii, bounding boxes, dates, sorting, and pagination inputs.

## Data rules
- Store event identity as UUID where compatible with the existing domain model.
- Store coordinates as a PostGIS geography(Point, 4326) value; longitude is X and latitude is Y when constructing points.
- Use GiST spatial index.
- Persist event status and event version / updated timestamp in the projection.
- Store UTC timestamps; convert to user locale in the frontend.
- Keep the existing Rain Cheque database and schema conventions where possible. Do not create duplicate event tables if the current Event Service already owns the event aggregate.

## Engineering rules
- Inspect the existing repository before changing dependencies, package names, endpoints, or folder structure.
- Reuse existing DTOs, authentication, error handling, logging, and frontend components where appropriate.
- Do not change unrelated features or existing API contracts.
- Add migrations for schema changes.
- Add unit, integration, and contract tests.
- Document environment variables and local startup instructions.
