package com.raincheque.exception;

import com.raincheque.dto.ErrorResponse;
import com.raincheque.payment.InvalidPaymentException;
import com.raincheque.payment.PaymentException;
import com.raincheque.payment.ProviderUnavailableException;
import com.raincheque.payment.RefundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Centralized exception handler for all controllers.
 * Ensures consistent error response structure and suppresses sensitive details in non-dev profiles.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Email already registered",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Invalid email or password",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            sanitizeMessage(ex.getMessage()),
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Validation failed",
            fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(cv -> {
                String propertyPath = cv.getPropertyPath().toString();
                String field = propertyPath.contains(".")
                    ? propertyPath.substring(propertyPath.lastIndexOf('.') + 1)
                    : propertyPath;
                return new ErrorResponse.FieldError(field, cv.getMessage());
            })
            .toList();

        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Validation failed",
            fieldErrors
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Access denied",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Authentication failed",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileType(InvalidFileTypeException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            sanitizeMessage(ex.getMessage()),
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(FileSizeLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileSizeLimitExceeded(FileSizeLimitExceededException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            sanitizeMessage(ex.getMessage()),
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            sanitizeMessage(ex.getMessage()),
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ProviderUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleProviderUnavailable(ProviderUnavailableException ex) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Payment provider is temporarily unavailable",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(InvalidPaymentException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Invalid payment operation",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ErrorResponse> handleRefundException(RefundException ex) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Refund processing failed",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            "Payment operation failed",
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = isDevProfile()
            ? ex.getMessage()
            : "An unexpected error occurred";

        ErrorResponse response = new ErrorResponse(
            Instant.now().toString(),
            status.value(),
            status.getReasonPhrase(),
            sanitizeMessage(message),
            null
        );
        return ResponseEntity.status(status).body(response);
    }

    /**
     * In non-dev profiles, sanitizes the message to remove class names, stack traces, SQL, and internal paths.
     * In dev profile, returns the message as-is for debugging convenience.
     */
    private String sanitizeMessage(String message) {
        if (isDevProfile() || message == null) {
            return message;
        }
        // Suppress class names (e.g., com.raincheque.something.ClassName)
        if (message.matches(".*\\b[a-z]+(\\.[a-z]+)+\\.[A-Z]\\w*.*")) {
            return "An unexpected error occurred";
        }
        // Suppress stack traces
        if (message.contains("\tat ") || message.contains(".java:")) {
            return "An unexpected error occurred";
        }
        // Suppress SQL queries
        if (message.toUpperCase().contains("SELECT ") || message.toUpperCase().contains("INSERT ")
            || message.toUpperCase().contains("UPDATE ") || message.toUpperCase().contains("DELETE ")) {
            return "An unexpected error occurred";
        }
        // Suppress internal file system paths
        if (message.contains("/src/") || message.contains("\\src\\")
            || message.matches(".*[A-Z]:\\\\.*") || message.matches(".*/home/.*")) {
            return "An unexpected error occurred";
        }
        return message;
    }

    /**
     * Checks if the current active profile is "dev".
     */
    private boolean isDevProfile() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
