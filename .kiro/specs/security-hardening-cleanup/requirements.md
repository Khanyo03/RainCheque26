# Requirements Document

## Introduction

This specification covers a comprehensive security hardening and code cleanup effort for the RainCheque application. The application consists of a React 18 frontend (Vite + TailwindCSS + Axios) and a Spring Boot 3.5 backend (Spring Security + JWT + JPA). The goal is to eliminate critical security vulnerabilities, resolve architectural inconsistencies, apply best practices, and prepare the application for production deployment.

## Glossary

- **Backend**: The Spring Boot 3.5 REST API server running on port 8081
- **Frontend**: The React 18 single-page application built with Vite, running on port 5173
- **Auth_Context**: A React context provider that manages authentication state (token, user info, login/logout actions) and exposes it to the component tree
- **Protected_Route**: A React route wrapper component that redirects unauthenticated users to the login page
- **Security_Filter_Chain**: The Spring Security HTTP filter configuration that defines authorization rules for endpoints
- **JWT_Service**: The Spring Boot service responsible for generating, validating, and parsing JSON Web Tokens
- **Global_Exception_Handler**: A Spring @ControllerAdvice class that provides centralized error handling for all controllers
- **Error_Boundary**: A React component that catches JavaScript errors in its child component tree and renders a fallback UI
- **Health_Endpoint**: A Spring Boot Actuator endpoint reporting application liveness and readiness status
- **TicketStatus_Enum**: The single canonical enum defining ticket lifecycle states (AVAILABLE, RESERVED, SOLD, USED, CANCELLED)
- **UserDetails_Service**: The single Spring Security UserDetailsService implementation that loads users from the database for authentication

## Requirements

### Requirement 1: Remove Hardcoded Secrets from Source Control

**User Story:** As a security engineer, I want all secrets removed from version-controlled files, so that credentials cannot be extracted from the repository history.

#### Acceptance Criteria

1. WHEN the Backend source is inspected, THE Backend SHALL load the JWT signing secret and the QR encryption secret exclusively from environment variables (with no hardcoded default fallback values in @Value annotations or application.yml), using the property keys `spring.security.jwt.secret` and `app.qr.secret` mapped to environment variables
2. WHEN the docker-compose.yml is inspected, THE docker-compose.yml SHALL reference environment variables (using `${VARIABLE_NAME}` syntax) for POSTGRES_PASSWORD and PGADMIN_DEFAULT_PASSWORD rather than containing plaintext values
3. WHEN the Frontend source is inspected, THE Frontend SHALL load all Firebase configuration values (apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId) from environment variables prefixed with VITE_, not from hardcoded strings in firebase.js
4. THE project root SHALL contain a .gitignore entry that excludes .env files at the project root, the frontend directory, and the docker directory
5. WHEN the Backend application starts without any required secret environment variable configured, THE Backend SHALL terminate during Spring context initialization (before accepting HTTP requests) and log an error message that names each missing variable
6. THE project root SHALL contain a .env.example file that lists all required environment variable names (without values) with comments describing their purpose, so that developers can identify which variables to configure

### Requirement 2: Delete the SimpleAuthController Backdoor

**User Story:** As a security engineer, I want the SimpleAuthController removed entirely, so that no endpoint exists that issues tokens without real credential validation.

#### Acceptance Criteria

1. WHEN the Backend codebase is inspected, THE Backend SHALL NOT contain the file SimpleAuthController.java nor any class annotated with @RequestMapping("/api/v1/simple-auth")
2. WHEN an unauthenticated request is made to /api/v1/simple-auth/login, THE Security_Filter_Chain SHALL return HTTP 401 (Unauthorized) because no permitAll rule exists for that path
3. WHEN an authenticated request is made to /api/v1/simple-auth/login, THE Backend SHALL return HTTP 404 because no controller handler is mapped to that path
4. WHEN the Backend codebase is inspected, THE Backend SHALL NOT contain any remaining import statements, bean references, or route registrations that reference SimpleAuthController

### Requirement 3: Remove or Gate TestController

**User Story:** As a security engineer, I want test-only endpoints removed from production builds, so that password hashes and unauthenticated user creation are never available in deployed environments.

#### Acceptance Criteria

1. WHEN the Backend runs with the "prod" profile active, THE Backend SHALL NOT register any endpoint under /api/v1/test, such that any request to paths matching /api/v1/test/** returns HTTP 404
2. IF the TestController is retained for development use, THEN THE TestController SHALL be annotated with @Profile("dev") so that Spring excludes it from all non-dev environments
3. WHILE the "dev" profile is active, WHEN the /api/v1/test/check-login endpoint is called, THE TestController SHALL NOT include password hashes, plaintext passwords, or encoded credentials in the response body
4. WHILE the "dev" profile is active, WHEN the /api/v1/test/create-test-user endpoint is called, THE TestController SHALL NOT return plaintext passwords or password hashes in the response body

### Requirement 4: Implement Frontend Route Protection

**User Story:** As a user, I want protected pages to require authentication, so that unauthenticated visitors cannot access my dashboard, tickets, or profile by navigating directly to the URL.

#### Acceptance Criteria

1. THE Frontend SHALL provide an Auth_Context that stores the current JWT token and user identity (user ID and email) in localStorage, and exposes login, logout, and isAuthenticated functions
2. WHEN an unauthenticated user (no valid token present in localStorage) navigates to /dashboard, /tickets, or /profile, THE Protected_Route SHALL redirect the user to /login and persist the originally requested path
3. WHEN a user successfully authenticates, THE Auth_Context SHALL store the JWT token and user identity in localStorage and redirect the user to the persisted originally-requested path, or to /dashboard if no prior path was stored
4. WHEN the stored token is expired or invalid (HTTP 401 from Backend), THE Auth_Context SHALL clear the token and user identity from localStorage and redirect the user to /login
5. WHEN the Frontend application initializes with a token present in localStorage, THE Auth_Context SHALL treat the user as authenticated and permit access to protected routes without requiring re-login

### Requirement 5: Remove Pre-filled Credentials from Login Form

**User Story:** As a security engineer, I want the login form to start empty, so that hardcoded credentials are never shipped in production bundles.

#### Acceptance Criteria

1. WHEN the login page renders, THE Frontend SHALL initialize the email and password form fields with empty strings as their React state default values, and SHALL NOT pre-populate them from localStorage, URL parameters, or any other source
2. THE Frontend source code SHALL NOT contain literal email addresses or passwords assigned as default values in form state initializers, input value attributes, or constant declarations used to populate login form fields
3. WHEN inspecting the login form's rendered DOM after initial page load, THE Frontend SHALL display both the email and password input fields with an empty value attribute (HTML placeholder hint text is permitted)

### Requirement 6: Consolidate Duplicate TicketStatus Enum

**User Story:** As a developer, I want a single canonical TicketStatus enum, so that there is no ambiguity about which enum to import.

#### Acceptance Criteria

1. THE Backend SHALL contain exactly one TicketStatus enum definition, located in the com.raincheque.entity package, declaring the values ACTIVE, USED, CANCELLED, and EXPIRED
2. WHEN the Backend source is compiled, THE Backend SHALL contain no file at the path com.raincheque.repository.TicketStatus
3. WHEN any Java source file in the Backend references TicketStatus, THE Backend SHALL resolve that reference to com.raincheque.entity.TicketStatus with zero compilation errors
4. IF a developer attempts to compile the Backend after consolidation, THEN THE Backend SHALL produce a successful build with no unresolved symbol errors related to TicketStatus

### Requirement 7: Consolidate UserDetailsService Implementations

**User Story:** As a developer, I want a single UserDetailsService implementation, so that the authentication pipeline has an unambiguous user-loading path.

#### Acceptance Criteria

1. THE Backend SHALL contain exactly one class implementing the Spring Security UserDetailsService interface, and all Spring Security configuration and filter beans SHALL inject that single implementation
2. THE UserDetails_Service SHALL load users by email from the UserRepository and return a Spring Security UserDetails object containing the user's email, password hash, and authorities derived from the User entity's Role enum using the format "ROLE_" followed by the role name
3. WHEN the JWT_Service authenticates a request, THE Security_Filter_Chain SHALL resolve @AuthenticationPrincipal to the application User entity (not Spring Security's User DTO)
4. IF the UserDetails_Service cannot find a user for the provided email, THEN THE UserDetails_Service SHALL throw a UsernameNotFoundException, causing the authentication filter to reject the request without setting a SecurityContext authentication

### Requirement 8: Fix @AuthenticationPrincipal Resolution

**User Story:** As a developer, I want @AuthenticationPrincipal to inject the JPA User entity, so that controllers can access user properties (id, role) without additional repository lookups.

#### Acceptance Criteria

1. WHEN the JwtAuthenticationFilter authenticates a request, THE Security_Filter_Chain SHALL place the JPA User entity (com.raincheque.entity.User) as the principal in the SecurityContext, with the entity's id, email, and role fields populated from the database
2. WHEN a controller method declares @AuthenticationPrincipal User user, THE Backend SHALL inject the JPA User entity such that user.getId(), user.getEmail(), and user.getRole() return the values stored in the database for the authenticated user
3. WHEN the JwtAuthenticationFilter authenticates a request, THE Security_Filter_Chain SHALL set the granted authorities on the Authentication token to match the role stored in the JPA User entity's role field
4. IF the JWT is valid but the corresponding user no longer exists in the database, THEN THE Security_Filter_Chain SHALL reject the request by not setting authentication in the SecurityContext and allowing the filter chain to continue unauthenticated

### Requirement 9: Disable MSW Mocks When Running Against Real Backend

**User Story:** As a developer, I want MSW disabled when connecting to the real backend, so that mock responses do not intercept legitimate API calls.

#### Acceptance Criteria

1. WHEN the Frontend starts in development mode with the VITE_ENABLE_MOCKS environment variable set to "false" or left unset, THE Frontend SHALL skip the dynamic import of the mocks module and SHALL NOT call worker.start()
2. WHEN the Frontend starts in development mode with the VITE_ENABLE_MOCKS environment variable set to "true", THE Frontend SHALL dynamically import the mocks module and call worker.start() before rendering the application
3. WHEN the Frontend starts in production mode, THE Frontend SHALL NOT execute the enableMocking code path regardless of environment variable values
4. IF the dynamic import of the mocks module fails during development with VITE_ENABLE_MOCKS set to "true", THEN THE Frontend SHALL log a warning message to the browser console and proceed to render the application without mocks within 1 second of the failure

### Requirement 10: Remove Unused TanStack Query or Integrate It

**User Story:** As a developer, I want dead dependencies removed, so that the bundle size is minimized and the codebase has a single clear data-fetching strategy.

#### Acceptance Criteria

1. IF no source file under src/ imports useQuery, useMutation, or useQueryClient from @tanstack/react-query, THEN THE Frontend SHALL remove @tanstack/react-query (and @tanstack/react-query-devtools if present) from package.json dependencies and remove the QueryClient instantiation and QueryClientProvider wrapper from main.jsx
2. IF TanStack Query is retained as the data-fetching strategy, THEN THE Frontend SHALL use useQuery or useMutation hooks in EventsPage, TicketsPage, and DashboardPage, replacing any direct Axios calls within those components with hook-based equivalents that expose loading, error, and data states
3. WHEN the chosen path (removal or integration) is applied, THE Frontend SHALL build without errors via the project's build command and render the application root without runtime exceptions related to missing providers or imports

### Requirement 11: Remove Duplicate api.js File

**User Story:** As a developer, I want a single API client module, so that request interceptors and base URL configuration are maintained in one place.

#### Acceptance Criteria

1. THE Frontend SHALL contain exactly one Axios instance definition, located at src/services/api.js
2. WHEN the Frontend source tree is inspected, THE Frontend SHALL NOT contain a file at src/api.js
3. WHEN the Frontend source tree is inspected, THE Frontend SHALL NOT contain any import statement that resolves to a path other than src/services/api.js for obtaining an Axios instance

### Requirement 12: Add a Global Exception Handler

**User Story:** As a developer, I want centralized exception handling, so that all API error responses follow a consistent structure and sensitive details are not leaked.

#### Acceptance Criteria

1. THE Backend SHALL provide a Global_Exception_Handler annotated with @ControllerAdvice
2. WHEN an unhandled exception occurs in any controller, THE Global_Exception_Handler SHALL return HTTP 500 with a JSON response containing a timestamp, HTTP status code, error type, and a generic message that does not reveal class names, stack traces, SQL queries, or internal system paths
3. WHEN a ResourceNotFoundException is thrown, THE Global_Exception_Handler SHALL return HTTP 404 with a JSON body following the standard error structure (timestamp, HTTP status code, error type, and message derived from the exception's constructor argument)
4. WHEN a validation error occurs (MethodArgumentNotValidException or ConstraintViolationException), THE Global_Exception_Handler SHALL return HTTP 400 with a JSON body following the standard error structure plus a list of field errors, each containing the field name and a description of the constraint violation
5. WHILE the Backend runs with a non-dev profile active, THE Global_Exception_Handler SHALL NOT include stack traces, internal exception class names, or raw exception messages in any error response body
6. WHEN a Spring Security AccessDeniedException or AuthenticationException propagates to the Global_Exception_Handler, THE Global_Exception_Handler SHALL return HTTP 403 or HTTP 401 respectively, with a JSON body following the standard error structure

### Requirement 13: Return DTOs Instead of JPA Entities

**User Story:** As a developer, I want API responses to use DTOs, so that internal entity structure changes do not break the public API contract and circular references are avoided.

#### Acceptance Criteria

1. WHEN the TicketController returns ticket data from GET /api/v1/tickets, GET /api/v1/tickets/{ticketId}, or POST /api/v1/tickets/validate, THE Backend SHALL map Ticket entities to a TicketResponse DTO before serialization
2. THE TicketResponse DTO SHALL NOT include the qrSecret field, hibernate proxy objects, or lazily-loaded association entities (booking, validatedBy, ticketTier) as nested JPA objects
3. THE TicketResponse DTO SHALL expose at minimum: id, ticketCode, status, seatNumber, pricePaid, usedAt, the associated event name, the tier name, and the booking ID as a scalar reference
4. WHEN the POST /api/v1/tickets/validate endpoint returns a validated ticket, THE TicketValidationResponse SHALL embed the TicketResponse DTO rather than the raw Ticket entity

### Requirement 14: Fix CompletableFuture and @Transactional Interaction

**User Story:** As a developer, I want batch ticket generation to execute within proper transaction boundaries, so that partial failures do not leave the database in an inconsistent state.

#### Acceptance Criteria

1. WHEN batch ticket generation runs asynchronously, THE BatchTicketService SHALL execute each booking's ticket generation as a separate transactional unit, where each unit commits independently upon success
2. IF an exception occurs while generating tickets for a single booking, THEN THE BatchTicketService SHALL roll back only that booking's ticket generation, persist the error details to the TicketBatchJob errorLog field, and continue processing remaining bookings
3. WHEN all bookings have been processed, THE BatchTicketService SHALL set the TicketBatchJob status to COMPLETED if all bookings succeeded, or FAILED if one or more bookings encountered errors
4. IF a batch operation encounters one or more booking failures, THEN THE BatchTicketService SHALL preserve all tickets that were successfully committed for other bookings in the same batch

### Requirement 15: Add React Error Boundary

**User Story:** As a user, I want the application to show a friendly error page when a component crashes, so that I can recover without manually refreshing.

#### Acceptance Criteria

1. THE Frontend SHALL wrap the main application routes in an Error_Boundary component that catches unhandled JavaScript errors occurring during rendering, lifecycle methods, and constructors of any descendant component
2. WHEN an unhandled JavaScript error occurs during rendering within the Error_Boundary, THE Error_Boundary SHALL replace the crashed component tree with a fallback UI that displays a non-technical error message (maximum 150 characters) and provides both a "Try Again" action that re-renders the failed component tree and a navigation link that returns the user to the home route
3. THE Error_Boundary SHALL NOT display raw stack traces, exception messages, or source file references to end users
4. WHEN the user activates the "Try Again" action on the error fallback UI, THE Error_Boundary SHALL clear its error state and attempt to re-render the previously failed component tree within 1 second of user activation
5. IF the re-render triggered by the "Try Again" action fails with another unhandled error, THEN THE Error_Boundary SHALL display the same fallback UI again without entering an infinite re-render loop

### Requirement 16: Implement Route-Based Code Splitting

**User Story:** As a user, I want pages to load on demand, so that the initial bundle is smaller and the application loads faster.

#### Acceptance Criteria

1. THE Frontend SHALL use React.lazy() and dynamic import() for all page-level components that require authentication or fetch data (DashboardPage, TicketsPage, ProfilePage, EventsPage, EventDetailPage), while keeping publicly accessed lightweight pages (HomePage, LoginPage, RegisterPage, NotFoundPage) in the main bundle
2. WHILE a lazily-loaded page is being fetched, THE Frontend SHALL display a visible loading indicator (spinner or skeleton placeholder) via a React Suspense fallback wrapping the route outlet
3. IF a lazily-loaded chunk fails to load due to a network error, THEN THE Frontend SHALL display an error message with a retry option that re-attempts the chunk load without requiring a full page refresh

### Requirement 17: Add Dockerfiles for Frontend and Backend

**User Story:** As a DevOps engineer, I want Dockerfiles for both services, so that the application can be built and deployed as container images.

#### Acceptance Criteria

1. THE Backend SHALL include a Dockerfile at the root of the backend-spring/rain-cheque-backend directory that uses a multi-stage build: a Maven build stage based on a Java 17 JDK image, followed by a runtime stage based on a Java 17 JRE Alpine image, and the runtime stage SHALL run the application as a non-root user on port 8081
2. THE Frontend SHALL include a Dockerfile at the root of the frontend/RAIN-CHEQUE directory that uses a multi-stage build: a Node build stage that runs the Vite production build, followed by a runtime stage based on an Nginx Alpine image that serves the built assets on port 80
3. WHEN docker build is run against each Dockerfile, THE build SHALL complete without errors and produce an image that starts a container which begins accepting HTTP connections within 30 seconds of container start
4. THE Backend directory and the Frontend directory SHALL each contain a .dockerignore file that excludes node_modules, .git, and build output directories from the Docker build context

### Requirement 18: Add Health Check Endpoint

**User Story:** As a DevOps engineer, I want a health check endpoint, so that orchestrators and load balancers can verify application liveness and readiness.

#### Acceptance Criteria

1. THE Backend SHALL expose a /actuator/health endpoint by including the spring-boot-starter-actuator dependency and configuring it to expose the health endpoint over HTTP
2. WHEN the application is healthy (database connectivity and disk space checks pass), THE Health_Endpoint SHALL return HTTP 200 with a JSON body containing a "status" field with value "UP" within 5 seconds of receiving the request
3. WHEN any health indicator reports a failure (e.g., database unreachable), THE Health_Endpoint SHALL return HTTP 503 with a JSON body containing a "status" field with value "DOWN"
4. THE Security_Filter_Chain SHALL permit unauthenticated access to /actuator/health by adding the path to the permitAll() request matchers
5. THE Backend SHALL NOT expose any other Actuator endpoints (e.g., /actuator/env, /actuator/beans) to unauthenticated access; only /actuator/health SHALL be publicly accessible

### Requirement 19: Add Spring Profiles for H2 and PostgreSQL

**User Story:** As a developer, I want separate Spring profiles for H2 (dev) and PostgreSQL (prod), so that the correct datasource is activated based on the deployment environment.

#### Acceptance Criteria

1. WHEN the "dev" profile is active, THE Backend SHALL connect to an H2 in-memory database with ddl-auto set to create-drop and the H2 web console enabled
2. WHEN the "prod" profile is active, THE Backend SHALL connect to PostgreSQL using credentials from the environment variables SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, and SPRING_DATASOURCE_PASSWORD, with ddl-auto set to validate and the H2 web console disabled
3. IF no Spring profile is explicitly specified at startup, THEN THE Backend SHALL activate the "dev" profile by default
4. IF the "prod" profile is active and any required datasource environment variable is not set, THEN THE Backend SHALL fail to start with an error message identifying the missing variable
5. WHEN the "prod" profile is active, THE Backend SHALL set show-sql to false and logging level for SQL statements to WARN or higher

### Requirement 20: Remove Dead Code and Development Artifacts

**User Story:** As a developer, I want dead code and leftover development artifacts removed, so that the codebase is clean and new contributors are not confused by unused files.

#### Acceptance Criteria

1. THE Frontend SHALL NOT contain the file src/api.js (duplicate API client)
2. THE Frontend SHALL NOT contain the file src/test-api.js (manual test script)
3. THE Frontend SHALL NOT contain the src/mocks/ directory or any of its contents
4. IF the application uses JWT authentication exclusively and no source file under src/ imports from src/firebase.js, THEN THE Frontend SHALL NOT contain src/firebase.js and SHALL NOT list the firebase npm package in package.json dependencies
5. THE Backend SHALL NOT contain debug-log.txt in version control
6. THE Backend .gitignore SHALL include an entry that excludes debug-log.txt from future commits
7. WHEN any dead code file is removed, THE Frontend SHALL NOT contain residual import statements or references to the removed files, and the production build SHALL complete without errors

### Requirement 21: Add CI/CD Configuration

**User Story:** As a developer, I want automated build and test pipelines, so that regressions are caught before merge and deployments are repeatable.

#### Acceptance Criteria

1. WHEN a pull request is opened or updated against the main branch, THE project SHALL trigger a GitHub Actions backend workflow (located in .github/workflows/) that checks out the code, sets up Java 17, builds the Backend using the Maven wrapper, and runs all tests
2. WHEN a pull request is opened or updated against the main branch, THE project SHALL trigger a GitHub Actions frontend workflow (located in .github/workflows/) that checks out the code, sets up Node.js 20, installs dependencies, runs linting, and builds the production bundle
3. IF any build, lint, or test step fails, THEN THE CI pipeline SHALL report a failing check status on the pull request, preventing merge while the status check is required
4. WHEN both workflows complete successfully, THE CI pipeline SHALL report a passing check status on the pull request
