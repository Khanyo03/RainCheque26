package com.raincheque.controller;

import com.raincheque.dto.EventRequest;
import com.raincheque.entity.Event;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for validation rejecting invalid requests with all errors collected.
 * Validates: Requirements 1.2, 3.2, 9.6
 *
 * Property: For any event request (create or update) with one or more invalid fields
 * (missing required fields, out-of-range lengths, blank category), the validator SHALL
 * reject the request and the error response SHALL contain a field-level error for every
 * invalid field — not just the first one encountered.
 */
// Feature: event-management, Property 2: Validation rejects invalid requests with all errors collected
class EventRequestValidationPropertyTest {

    private Validator validator;

    @BeforeProperty
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Property(tries = 100)
    // Feature: event-management, Property 2: Validation rejects invalid requests with all errors collected
    void invalidRequestCollectsAllFieldErrors(
            @ForAll("invalidEventRequests") InvalidEventRequestWithExpectedCount requestWithCount) {

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(requestWithCount.request());

        // The validator must report at least as many violations as we introduced invalid fields
        assertThat(violations)
                .as("Expected at least %d violations for %d invalid fields, but got %d. Invalid fields: %s",
                        requestWithCount.expectedMinViolations(),
                        requestWithCount.expectedMinViolations(),
                        violations.size(),
                        requestWithCount.invalidFieldNames())
                .hasSizeGreaterThanOrEqualTo(requestWithCount.expectedMinViolations());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 2: Validation rejects invalid requests with all errors collected
    void invalidRequestIsAlwaysRejected(
            @ForAll("invalidEventRequests") InvalidEventRequestWithExpectedCount requestWithCount) {

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(requestWithCount.request());

        // The request must always be rejected (have at least one violation)
        assertThat(violations)
                .as("Request with invalid fields %s should be rejected", requestWithCount.invalidFieldNames())
                .isNotEmpty();
    }

    // --- Data carrier for request + expected violation count ---

    record InvalidEventRequestWithExpectedCount(
            EventRequest request,
            int expectedMinViolations,
            List<String> invalidFieldNames
    ) {}

    // --- Generators ---

    @Provide
    Arbitrary<InvalidEventRequestWithExpectedCount> invalidEventRequests() {
        // Generate a subset of fields to invalidate (at least 1)
        return Arbitraries.integers().between(1, 5).flatMap(numInvalidFields -> {
            // Pick which fields to invalidate from the 5 key required fields
            // Fields: title, venueId, startTime, endTime, category
            return Arbitraries.shuffle(List.of("title", "venueId", "startTime", "endTime", "category"))
                    .flatMap(shuffledFields -> {
                        List<String> fieldsToInvalidate = shuffledFields.subList(0, numInvalidFields);
                        return buildInvalidRequest(fieldsToInvalidate);
                    });
        });
    }

    private Arbitrary<InvalidEventRequestWithExpectedCount> buildInvalidRequest(List<String> fieldsToInvalidate) {
        // Generate valid base values first, then override invalid ones
        Arbitrary<String> validTitles = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50);
        Arbitrary<Long> validVenueIds = Arbitraries.longs().between(1L, 1000L);
        Arbitrary<LocalDateTime> validStartTimes = Arbitraries.longs()
                .between(1, 365)
                .map(days -> LocalDateTime.now().plusDays(days));
        Arbitrary<String> validCategories = Arbitraries.of(
                "Music", "Sports", "Comedy", "Theatre", "Conference");

        return Combinators.combine(validTitles, validVenueIds, validStartTimes, validCategories)
                .as((title, venueId, startTime, category) -> {
                    String finalTitle = title;
                    Long finalVenueId = venueId;
                    LocalDateTime finalStartTime = startTime;
                    LocalDateTime finalEndTime = startTime.plusHours(2);
                    String finalCategory = category;

                    int expectedViolations = 0;
                    List<String> invalidFieldNames = new ArrayList<>();

                    for (String field : fieldsToInvalidate) {
                        switch (field) {
                            case "title" -> {
                                finalTitle = ""; // Blank title violates @NotBlank
                                expectedViolations++;
                                invalidFieldNames.add("title");
                            }
                            case "venueId" -> {
                                finalVenueId = null; // Null venue violates @NotNull
                                expectedViolations++;
                                invalidFieldNames.add("venueId");
                            }
                            case "startTime" -> {
                                finalStartTime = null; // Null startTime violates @NotNull
                                expectedViolations++;
                                invalidFieldNames.add("startTime");
                            }
                            case "endTime" -> {
                                finalEndTime = null; // Null endTime violates @NotNull
                                expectedViolations++;
                                invalidFieldNames.add("endTime");
                            }
                            case "category" -> {
                                finalCategory = "  "; // Blank category violates @NotBlank
                                expectedViolations++;
                                invalidFieldNames.add("category");
                            }
                        }
                    }

                    EventRequest request = new EventRequest(
                            finalTitle,
                            "Valid description",
                            "Short desc",
                            finalVenueId,
                            finalStartTime,
                            finalEndTime,
                            finalCategory,
                            false,
                            null
                    );

                    return new InvalidEventRequestWithExpectedCount(
                            request, expectedViolations, Collections.unmodifiableList(invalidFieldNames));
                });
    }
}
