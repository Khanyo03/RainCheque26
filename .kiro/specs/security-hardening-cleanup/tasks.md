# Implementation Plan: Security Hardening & Cleanup

## Overview

This plan implements a comprehensive security hardening and code cleanup across the RainCheque application, organized into four groups by priority: Security (Critical), Architecture, Best Practices, and Deployment. Each task is a discrete coding step that builds on previous work, with checkpoints for validation.

## Tasks

- [x] 1. Security (Critical) — Secrets, Backdoor Removal, Route Protection
  - [x] 1.1 Externalize backend secrets from application.yml
    - Open `backend-spring/rain-cheque-backend/src/main/resources/application.yml` and replace hardcoded `jwt.secret` and `app.qr.secret` values with `${SPRING_SECURITY_JWT_SECRET}` and `${APP_QR_SECRET}` placeholder references (no default values)
    - Create `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/config/SecretsValidationConfig.java` — a `@Configuration` class with `@PostConstruct` that checks required env vars are present in non-dev profiles and throws `IllegalStateException` with the missing variable names if absent
    - _Requirements: 1.1, 1.5_

  - [x] 1.2 Externalize docker-compose and create .env.example
    - Update `docker-compose.yml` to replace plaintext `POSTGRES_PASSWORD` and `PGADMIN_DEFAULT_PASSWORD` values with `${POSTGRES_PASSWORD}` and `${PGADMIN_DEFAULT_PASSWORD}` variable references
    - Create `.env.example` at the project root listing all required env vars (SPRING_SECURITY_JWT_SECRET, APP_QR_SECRET, POSTGRES_PASSWORD, PGADMIN_DEFAULT_PASSWORD, SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD, VITE_ENABLE_MOCKS) with descriptive comments
    - Add `.env` exclusion entries to the project-root `.gitignore`, `frontend/RAIN-CHEQUE/.gitignore`, and `docker/.gitignore` (or the appropriate docker directory gitignore)
    - _Requirements: 1.2, 1.4, 1.6_

  - [x] 1.3 Delete SimpleAuthController and close the security gap
    - Delete `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/controller/SimpleAuthController.java`
    - In `SecurityConfig.java`, remove any `requestMatchers` rule that permits `/api/v1/simple-auth/**`
    - Search the codebase for any remaining imports or references to `SimpleAuthController` and remove them
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 1.4 Gate TestController behind @Profile("dev") and sanitize responses
    - Add `@Profile("dev")` annotation to `TestController.java`
    - Remove any response fields that expose password hashes or plaintext passwords from `check-login` and `create-test-user` endpoints
    - In `SecurityConfig.java`, remove the `.requestMatchers("/api/v1/test/**").permitAll()` rule — in dev profile, the test controller registers its own routes; in prod it won't exist at all
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [x] 1.5 Remove pre-filled credentials from LoginPage
    - Open `frontend/RAIN-CHEQUE/src/pages/LoginPage.jsx` and change any state initializers for email/password from pre-filled values to empty strings (`''`)
    - Remove any constant declarations or default value assignments that contain literal email/password values
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 1.6 Implement AuthContext and ProtectedRoute in frontend
    - Create `frontend/RAIN-CHEQUE/src/context/AuthContext.jsx` exporting `AuthProvider` and `useAuth` hook with `token`, `user`, `isAuthenticated`, `login(token, user)`, `logout()` — backed by localStorage
    - Create `frontend/RAIN-CHEQUE/src/components/ProtectedRoute.jsx` that reads `isAuthenticated` from context, saves `location.pathname`, and redirects to `/login` if unauthenticated
    - Update `frontend/RAIN-CHEQUE/src/services/api.js` Axios response interceptor to call `logout()` on HTTP 401
    - Wrap `<App />` in `<AuthProvider>` in `main.jsx`
    - Update `App.jsx` to wrap `/dashboard`, `/tickets`, `/profile` routes with `<ProtectedRoute>`
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [x] 1.7 Write unit tests for AuthContext and ProtectedRoute
    - Create `frontend/RAIN-CHEQUE/src/context/AuthContext.test.jsx` testing login/logout/isAuthenticated behavior
    - Create `frontend/RAIN-CHEQUE/src/components/ProtectedRoute.test.jsx` testing redirect for unauthenticated users
    - _Requirements: 4.1, 4.2_

- [x] 2. Checkpoint — Verify security-critical changes
  - Ensure all backend tests pass with `./mvnw test` (run with `-Dspring.profiles.active=dev`)
  - Ensure frontend builds with `npm run build` in `frontend/RAIN-CHEQUE`
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Architecture — Enum Consolidation, UserDetailsService, @AuthenticationPrincipal
  - [x] 3.1 Consolidate duplicate TicketStatus enum
    - Verify `com.raincheque.entity.TicketStatus` contains the canonical values (ACTIVE, USED, CANCELLED, EXPIRED)
    - Delete `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/repository/TicketStatus.java`
    - Update all import statements across the codebase that referenced `com.raincheque.repository.TicketStatus` to use `com.raincheque.entity.TicketStatus`
    - Verify compilation succeeds with `./mvnw compile`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

  - [x] 3.2 Consolidate to single UserDetailsService implementation
    - In `CustomUserDetailsService.java`, ensure it is the sole `@Service` implementing `UserDetailsService` — load user by email from `UserRepository`, return Spring Security `UserDetails` with `ROLE_` + role name authorities
    - Remove the `implements UserDetailsService` and `loadUserByUsername` method from `UserService.java`
    - Update `JwtAuthenticationFilter.java` to inject `CustomUserDetailsService` (or `UserRepository` directly) instead of `UserService` for user loading
    - _Requirements: 7.1, 7.2, 7.4_

  - [x] 3.3 Fix @AuthenticationPrincipal to inject JPA User entity
    - Modify `JwtAuthenticationFilter.doFilterInternal` to: after validating the JWT, load the JPA `User` entity via `UserRepository.findByEmail()`, and set it as the principal in `UsernamePasswordAuthenticationToken(user, null, authorities)` where authorities are derived from `user.getRole()`
    - Remove the `/api/v1/test/` skip from the filter's `doFilterInternal` (no longer needed since TestController is profile-gated)
    - If the user entity is not found for a valid JWT, skip setting authentication (allow filter chain to continue unauthenticated)
    - _Requirements: 7.3, 8.1, 8.2, 8.3, 8.4_

  - [x] 3.4 Write integration tests for security filter chain
    - Create `backend-spring/rain-cheque-backend/src/test/java/com/raincheque/security/SecurityFilterChainIntegrationTest.java` using `@SpringBootTest` + `MockMvc`
    - Test: `/api/v1/simple-auth/login` returns 401 (unauthenticated) or 404
    - Test: `/api/v1/test/**` returns 404 under prod profile
    - Test: `/actuator/health` is publicly accessible
    - Test: `@AuthenticationPrincipal` resolves to JPA User entity
    - _Requirements: 2.2, 3.1, 8.2, 18.4_

- [x] 4. Checkpoint — Verify architecture changes compile and pass tests
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Best Practices — Exception Handler, DTOs, Transaction Fix, Frontend Cleanup
  - [x] 5.1 Add GlobalExceptionHandler and ErrorResponse DTO
    - Create `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/dto/ErrorResponse.java` as a Java record with fields: `timestamp`, `status`, `error`, `message`, `fieldErrors` (list of nested `FieldError` records)
    - Create `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/exception/GlobalExceptionHandler.java` annotated with `@ControllerAdvice`
    - Handle: `ResourceNotFoundException` → 404, `MethodArgumentNotValidException` / `ConstraintViolationException` → 400 with field errors, `AccessDeniedException` → 403, `AuthenticationException` → 401, generic `Exception` → 500
    - In non-dev profiles, suppress class names, stack traces, SQL, and internal paths from error messages
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6_

  - [x] 5.2 Write property test for Exception Response Structure (Property 1)
    - **Property 1: Exception Response Structure Consistency**
    - Create `backend-spring/rain-cheque-backend/src/test/java/com/raincheque/exception/GlobalExceptionHandlerPropertyTest.java`
    - Add jqwik dependency to pom.xml (`net.jqwik:jqwik:1.9.1` scope test)
    - Generate random exceptions (RuntimeException, ResourceNotFoundException, ConstraintViolationException, AccessDeniedException, AuthenticationException) and verify response always contains `timestamp`, `status`, `error`, `message` and never contains Java class names, stack traces, SQL, or file paths
    - `@Property(tries = 100)` with comment tag: `// Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency`
    - **Validates: Requirements 12.2, 12.5**

  - [x] 5.3 Create TicketResponse DTO and refactor TicketController
    - Create `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/dto/TicketResponse.java` as a Java record with static `from(Ticket)` factory method mapping id, ticketCode, status, seatNumber, pricePaid, usedAt, eventName, tierName, bookingId
    - Update `TicketValidationResponse` to embed `TicketResponse` instead of raw `Ticket` entity
    - Update `TicketController` GET /tickets, GET /tickets/{id}, and POST /tickets/validate to map entities to `TicketResponse` before returning
    - _Requirements: 13.1, 13.2, 13.3, 13.4_

  - [x] 5.4 Write property test for Ticket DTO Mapping (Property 2)
    - **Property 2: Ticket DTO Mapping Completeness and Safety**
    - Create `backend-spring/rain-cheque-backend/src/test/java/com/raincheque/dto/TicketResponsePropertyTest.java`
    - Generate random Ticket entities with populated associations, map to TicketResponse, assert all fields non-null and matching source, assert serialized JSON does not contain `qrSecret` or JPA proxy class names
    - `@Property(tries = 100)` with comment tag: `// Feature: security-hardening-cleanup, Property 2: Ticket DTO Mapping Completeness and Safety`
    - **Validates: Requirements 13.1, 13.2, 13.3**

  - [x] 5.5 Fix BatchTicketService transaction boundaries
    - Create `backend-spring/rain-cheque-backend/src/main/java/com/raincheque/service/BookingTicketGenerator.java` — a `@Service` with method `generateTicketsForBooking(Booking)` annotated `@Transactional(propagation = Propagation.REQUIRES_NEW)`
    - Refactor `BatchTicketService.batchGenerateEventTickets` to: save the job, then `CompletableFuture.runAsync()` iterating bookings, calling `bookingTicketGenerator.generateTicketsForBooking(booking)` per booking in a try/catch that logs failures to `job.errorLog`
    - After loop, set job status to COMPLETED (all success) or FAILED (any failures), save job
    - Remove the `@Transactional` annotation from `batchGenerateEventTickets` (the outer method runs async and delegates transactions to the inner bean)
    - _Requirements: 14.1, 14.2, 14.3, 14.4_

  - [x] 5.6 Write property test for Batch Transaction Isolation (Property 3)
    - **Property 3: Batch Transaction Isolation**
    - Create `backend-spring/rain-cheque-backend/src/test/java/com/raincheque/service/BatchTransactionIsolationPropertyTest.java`
    - Generate random sets of bookings, inject failures on random subset, verify: successful bookings' tickets are preserved, failed bookings' tickets are rolled back, errors logged in errorLog
    - `@Property(tries = 100)` with comment tag: `// Feature: security-hardening-cleanup, Property 3: Batch Transaction Isolation`
    - **Validates: Requirements 14.1, 14.2, 14.4**

  - [x] 5.7 Write property test for Batch Job Final Status (Property 4)
    - **Property 4: Batch Job Final Status Determination**
    - Create `backend-spring/rain-cheque-backend/src/test/java/com/raincheque/service/BatchJobStatusPropertyTest.java`
    - Generate random success/failure combinations, verify COMPLETED iff all succeed, FAILED iff any failure
    - `@Property(tries = 100)` with comment tag: `// Feature: security-hardening-cleanup, Property 4: Batch Job Final Status Determination`
    - **Validates: Requirements 14.3**

  - [x] 5.8 Gate MSW behind VITE_ENABLE_MOCKS environment variable
    - Update `frontend/RAIN-CHEQUE/src/main.jsx` `enableMocking()` function to check `import.meta.env.PROD` (return early), then `import.meta.env.VITE_ENABLE_MOCKS !== 'true'` (return early), then attempt dynamic import with try/catch that logs warning
    - Remove the `QueryClient` instantiation and `QueryClientProvider` wrapper from `main.jsx`
    - Remove `@tanstack/react-query` from `package.json` dependencies
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 10.1_

  - [x] 5.9 Remove dead code and unused files from frontend
    - Delete `frontend/RAIN-CHEQUE/src/api.js` (duplicate Axios instance)
    - Delete `frontend/RAIN-CHEQUE/src/test-api.js` (manual test script)
    - Delete `frontend/RAIN-CHEQUE/src/firebase.js` (hardcoded Firebase config)
    - Remove `firebase` from `package.json` dependencies
    - Update any imports that referenced `src/api.js` or `src/firebase.js` to use `src/services/api.js` instead
    - Verify no residual imports referencing deleted files remain
    - _Requirements: 11.1, 11.2, 11.3, 20.1, 20.2, 20.4, 20.7_

  - [x] 5.10 Add Error Boundary component to frontend
    - Create `frontend/RAIN-CHEQUE/src/components/ErrorBoundary.jsx` — React class component using `componentDidCatch` and `getDerivedStateFromError`
    - Render fallback UI with generic error message (≤150 chars), "Try Again" button (resets `hasError` state), and "Go Home" link (navigates to `/`)
    - Wrap main route outlet in `App.jsx` with `<ErrorBoundary>`
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.5_

  - [x] 5.11 Implement route-based code splitting
    - In `App.jsx`, convert DashboardPage, TicketsPage, ProfilePage, EventsPage, EventDetailPage imports to `React.lazy(() => import(...))`
    - Keep HomePage, LoginPage, RegisterPage, NotFoundPage as static imports
    - Wrap the route outlet with `<Suspense fallback={<LoadingSpinner />}>`
    - Create `frontend/RAIN-CHEQUE/src/components/LoadingSpinner.jsx` — a simple spinner component
    - _Requirements: 16.1, 16.2, 16.3_

  - [x] 5.12 Write unit tests for ErrorBoundary and LoginPage
    - Create `frontend/RAIN-CHEQUE/src/components/ErrorBoundary.test.jsx` testing fallback rendering and retry behavior
    - Create or update `frontend/RAIN-CHEQUE/src/pages/LoginPage.test.jsx` verifying empty initial form state
    - _Requirements: 15.2, 15.4, 5.1_

- [x] 6. Checkpoint — Verify best practices changes
  - Run `./mvnw test` in backend-spring/rain-cheque-backend
  - Run `npm run test` in frontend/RAIN-CHEQUE
  - Run `npm run build` in frontend/RAIN-CHEQUE to verify no dead imports
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. Deployment — Dockerfiles, Health Checks, Profiles, CI/CD
  - [x] 7.1 Add Spring Boot Actuator and health endpoint
    - Add `spring-boot-starter-actuator` dependency to `pom.xml`
    - In `application.yml`, configure `management.endpoints.web.exposure.include=health` (expose only health)
    - In `SecurityConfig.java`, add `/actuator/health` to `permitAll()` matchers
    - _Requirements: 18.1, 18.2, 18.3, 18.4, 18.5_

  - [x] 7.2 Create Spring profile configuration files
    - Create `backend-spring/rain-cheque-backend/src/main/resources/application-dev.yml` — H2 datasource, ddl-auto=create-drop, H2 console enabled
    - Create `backend-spring/rain-cheque-backend/src/main/resources/application-prod.yml` — PostgreSQL via env vars, ddl-auto=validate, show-sql=false, H2 console disabled, SQL logging at WARN
    - Set default profile to `dev` in the base `application.yml` (`spring.profiles.active: dev`)
    - _Requirements: 19.1, 19.2, 19.3, 19.4, 19.5_

  - [x] 7.3 Remove backend dead code and add .gitignore entries
    - Delete `backend-spring/rain-cheque-backend/debug-log.txt`
    - Add `debug-log.txt` entry to `backend-spring/rain-cheque-backend/.gitignore`
    - Delete the `frontend/RAIN-CHEQUE/src/mocks/` directory (MSW handlers removed per Req 20.3)
    - Remove any residual MSW import references from `main.jsx` (the enableMocking function should handle dynamic import gracefully when mocks dir is absent — update to wrap in try/catch if not already)
    - _Requirements: 20.3, 20.5, 20.6_

  - [x] 7.4 Create Backend Dockerfile and .dockerignore
    - Create `backend-spring/rain-cheque-backend/Dockerfile` — multi-stage: Stage 1 uses `eclipse-temurin:17-jdk` for Maven build (dependency:go-offline + package -DskipTests), Stage 2 uses `eclipse-temurin:17-jre-alpine` with non-root user, exposes 8081
    - Create `backend-spring/rain-cheque-backend/.dockerignore` excluding `.git`, `target/`, `debug-log.txt`, `.env`
    - _Requirements: 17.1, 17.3, 17.4_

  - [x] 7.5 Create Frontend Dockerfile, .dockerignore, and nginx.conf
    - Create `frontend/RAIN-CHEQUE/Dockerfile` — multi-stage: Stage 1 uses `node:20-alpine` for `npm ci` + `npm run build`, Stage 2 uses `nginx:alpine` serving from `/usr/share/nginx/html`, exposes 80
    - Create `frontend/RAIN-CHEQUE/nginx.conf` with try_files fallback for SPA routing
    - Create `frontend/RAIN-CHEQUE/.dockerignore` excluding `node_modules`, `.git`, `dist/`
    - _Requirements: 17.2, 17.3, 17.4_

  - [x] 7.6 Create GitHub Actions CI/CD workflows
    - Create `.github/workflows/backend-ci.yml` — triggers on PR to main, checks out code, sets up Java 17, runs `./mvnw verify -B` in `backend-spring/rain-cheque-backend`
    - Create `.github/workflows/frontend-ci.yml` — triggers on PR to main, checks out code, sets up Node 20, runs `npm ci`, `npm run build`, `npm run test` in `frontend/RAIN-CHEQUE`
    - _Requirements: 21.1, 21.2, 21.3, 21.4_

- [x] 8. Final Checkpoint — Full verification
  - Run `./mvnw verify` in backend to confirm compilation + all tests pass
  - Run `npm run build` and `npm run test` in frontend to confirm clean build
  - Verify no hardcoded secrets remain with a grep for known patterns
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation between groups
- Property tests validate the 4 correctness properties from the design (jqwik on JVM)
- The design uses Java 17 (backend) and JavaScript/React (frontend) — no language selection needed
- Group 1 (Security) must be completed first as subsequent groups depend on SecurityConfig and auth changes
- Group 3 (Architecture) depends on Group 1 security removals being complete (SimpleAuthController gone, test paths removed)
- Group 5 (Best Practices) builds on the consolidated architecture from Group 3
- Group 7 (Deployment) can proceed mostly independently but benefits from all prior changes being stable

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.5"] },
    { "id": 1, "tasks": ["1.3", "1.4"] },
    { "id": 2, "tasks": ["1.6", "3.1"] },
    { "id": 3, "tasks": ["1.7", "3.2"] },
    { "id": 4, "tasks": ["3.3"] },
    { "id": 5, "tasks": ["3.4", "5.1", "5.8"] },
    { "id": 6, "tasks": ["5.2", "5.3", "5.9"] },
    { "id": 7, "tasks": ["5.4", "5.5", "5.10"] },
    { "id": 8, "tasks": ["5.6", "5.7", "5.11"] },
    { "id": 9, "tasks": ["5.12", "7.1", "7.2"] },
    { "id": 10, "tasks": ["7.3", "7.4", "7.5"] },
    { "id": 11, "tasks": ["7.6"] }
  ]
}
```
