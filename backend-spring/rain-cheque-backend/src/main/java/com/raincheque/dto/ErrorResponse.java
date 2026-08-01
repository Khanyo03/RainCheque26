package com.raincheque.dto;

import java.util.List;

/**
 * Standard error response DTO for all API error responses.
 * Provides a consistent structure: timestamp, HTTP status, error type, message, and optional field errors.
 */
public record ErrorResponse(
    String timestamp,
    int status,
    String error,
    String message,
    List<FieldError> fieldErrors
) {
    /**
     * Represents a single field validation error.
     */
    public record FieldError(String field, String message) {}
}
