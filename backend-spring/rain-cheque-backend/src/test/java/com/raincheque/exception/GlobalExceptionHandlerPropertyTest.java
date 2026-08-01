package com.raincheque.exception;

import com.raincheque.dto.ErrorResponse;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import java.util.Set;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for GlobalExceptionHandler.
 * Validates: Requirements 12.2, 12.5
 */
// Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
class GlobalExceptionHandlerPropertyTest {

    private GlobalExceptionHandler handler;

    @BeforeProperty
    void setUp() {
        // Mock environment to simulate non-dev profile (empty active profiles)
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        handler = new GlobalExceptionHandler(environment);
    }

    // Patterns that should NEVER appear in non-dev error responses
    private static final Pattern JAVA_CLASS_NAME_PATTERN =
        Pattern.compile("\\b[a-z]+(\\.[a-z]+)+\\.[A-Z]\\w*");
    private static final Pattern STACK_TRACE_PATTERN =
        Pattern.compile("\\tat |java:\\d+");
    private static final Pattern SQL_PATTERN =
        Pattern.compile("(?i)\\b(SELECT|INSERT|UPDATE|DELETE)\\s");
    private static final Pattern FILE_PATH_PATTERN =
        Pattern.compile("(/src/|\\\\src\\\\|[A-Z]:\\\\|/home/)");

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
    void runtimeExceptionAlwaysProducesValidStructureWithoutLeaks(
            @ForAll("runtimeExceptionMessages") String message) {

        RuntimeException ex = new RuntimeException(message);
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertValidResponseStructure(response);
        assertNoSensitiveInfoLeak(response.getBody());
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
    void resourceNotFoundExceptionAlwaysProducesValidStructureWithoutLeaks(
            @ForAll("exceptionMessages") String message) {

        ResourceNotFoundException ex = new ResourceNotFoundException(message);
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertValidResponseStructure(response);
        assertNoSensitiveInfoLeak(response.getBody());
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
    void constraintViolationExceptionAlwaysProducesValidStructureWithoutLeaks(
            @ForAll("constraintViolationMessages") String fieldMessage) {

        // Create a mock ConstraintViolation
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("fieldName");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(fieldMessage);

        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));
        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        assertValidResponseStructure(response);
        assertNoSensitiveInfoLeak(response.getBody());
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
    void accessDeniedExceptionAlwaysProducesValidStructureWithoutLeaks(
            @ForAll("exceptionMessages") String message) {

        AccessDeniedException ex = new AccessDeniedException(message);
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex);

        assertValidResponseStructure(response);
        assertNoSensitiveInfoLeak(response.getBody());
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 1: Exception Response Structure Consistency
    void authenticationExceptionAlwaysProducesValidStructureWithoutLeaks(
            @ForAll("exceptionMessages") String message) {

        // AuthenticationException is abstract, create a concrete subclass
        AuthenticationException ex = new AuthenticationException(message) {};
        ResponseEntity<ErrorResponse> response = handler.handleAuthentication(ex);

        assertValidResponseStructure(response);
        assertNoSensitiveInfoLeak(response.getBody());
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    // --- Assertion Helpers ---

    private void assertValidResponseStructure(ResponseEntity<ErrorResponse> response) {
        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();

        ErrorResponse body = response.getBody();
        assertThat(body.timestamp()).isNotNull().isNotBlank();
        assertThat(body.status()).isGreaterThan(0);
        assertThat(body.error()).isNotNull().isNotBlank();
        assertThat(body.message()).isNotNull();
    }

    private void assertNoSensitiveInfoLeak(ErrorResponse body) {
        String message = body.message();

        assertThat(JAVA_CLASS_NAME_PATTERN.matcher(message).find())
            .as("Message should not contain Java class names: %s", message)
            .isFalse();
        assertThat(STACK_TRACE_PATTERN.matcher(message).find())
            .as("Message should not contain stack traces: %s", message)
            .isFalse();
        assertThat(SQL_PATTERN.matcher(message).find())
            .as("Message should not contain SQL queries: %s", message)
            .isFalse();
        assertThat(FILE_PATH_PATTERN.matcher(message).find())
            .as("Message should not contain file paths: %s", message)
            .isFalse();
    }

    // --- Providers / Generators ---

    @Provide
    Arbitrary<String> exceptionMessages() {
        return Arbitraries.oneOf(
            // Safe messages that should pass through
            Arbitraries.of(
                "Resource not found",
                "User with id 42 not found",
                "Event does not exist",
                "Ticket already used",
                "Invalid booking reference"
            ),
            // Messages that contain Java class names (should be sanitized)
            Arbitraries.of(
                "com.raincheque.entity.User not found",
                "org.hibernate.LazyInitializationException: could not initialize proxy",
                "java.lang.NullPointerException at com.raincheque.service.TicketService",
                "com.raincheque.exception.ResourceNotFoundException: Entity missing"
            ),
            // Messages with SQL (should be sanitized)
            Arbitraries.of(
                "SELECT * FROM users WHERE email = 'test@example.com'",
                "INSERT INTO tickets (id, code) VALUES (1, 'T001')",
                "UPDATE bookings SET status = 'CANCELLED' WHERE id = 5",
                "DELETE FROM sessions WHERE expired = true"
            ),
            // Messages with file paths (should be sanitized)
            Arbitraries.of(
                "/src/main/java/com/raincheque/service/TicketService.java:42",
                "C:\\Users\\dev\\projects\\raincheque\\src\\main\\java",
                "/home/deploy/raincheque/logs/error.log",
                "Error in /src/resources/application.yml"
            ),
            // Messages with stack traces (should be sanitized)
            Arbitraries.of(
                "\tat com.raincheque.service.TicketService.validate(TicketService.java:55)",
                "Exception occurred\n\tat org.springframework.web.servlet.DispatcherServlet.doDispatch",
                "Error at TicketController.java:123"
            ),
            // Random alphanumeric strings (should pass through safely)
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100)
        );
    }

    @Provide
    Arbitrary<String> runtimeExceptionMessages() {
        return exceptionMessages();
    }

    @Provide
    Arbitrary<String> constraintViolationMessages() {
        return Arbitraries.oneOf(
            // Typical validation messages (should be safe)
            Arbitraries.of(
                "must not be blank",
                "must be a valid email address",
                "size must be between 1 and 255",
                "must be positive",
                "must not be null"
            ),
            // Random alphanumeric strings
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
        );
    }
}
