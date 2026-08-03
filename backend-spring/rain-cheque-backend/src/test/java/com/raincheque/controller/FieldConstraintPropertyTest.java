package com.raincheque.controller;

import com.raincheque.dto.EventRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for field constraint enforcement.
 * Validates: Requirements 9.1, 9.2, 9.3, 9.7
 *
 * For any string value for title with length outside [3, 200], or description exceeding
 * 10000 characters, or shortDescription exceeding 500 characters, or category that is
 * blank/whitespace-only, the validator SHALL reject the event request.
 */
// Feature: event-management, Property 4: Field constraint enforcement
class FieldConstraintPropertyTest {

    private Validator validator;

    @BeforeProperty
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Property(tries = 100)
    // Feature: event-management, Property 4: Field constraint enforcement
    void titleTooShort(
            @ForAll("tooShortTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId,
            @ForAll("futureStartTimes") LocalDateTime startTime) {

        LocalDateTime endTime = startTime.plusHours(2);

        EventRequest request = new EventRequest(
                title,
                "A valid description",
                "A valid short description",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        Set<String> violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violatedPaths).contains("title");
    }

    @Property(tries = 100)
    // Feature: event-management, Property 4: Field constraint enforcement
    void titleTooLong(
            @ForAll("tooLongTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId,
            @ForAll("futureStartTimes") LocalDateTime startTime) {

        LocalDateTime endTime = startTime.plusHours(2);

        EventRequest request = new EventRequest(
                title,
                "A valid description",
                "A valid short description",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        Set<String> violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violatedPaths).contains("title");
    }

    @Property(tries = 100)
    // Feature: event-management, Property 4: Field constraint enforcement
    void descriptionTooLong(
            @ForAll("tooLongDescriptions") String description,
            @ForAll("validTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId,
            @ForAll("futureStartTimes") LocalDateTime startTime) {

        LocalDateTime endTime = startTime.plusHours(2);

        EventRequest request = new EventRequest(
                title,
                description,
                "A valid short description",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        Set<String> violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violatedPaths).contains("description");
    }

    @Property(tries = 100)
    // Feature: event-management, Property 4: Field constraint enforcement
    void shortDescriptionTooLong(
            @ForAll("tooLongShortDescriptions") String shortDescription,
            @ForAll("validTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId,
            @ForAll("futureStartTimes") LocalDateTime startTime) {

        LocalDateTime endTime = startTime.plusHours(2);

        EventRequest request = new EventRequest(
                title,
                "A valid description",
                shortDescription,
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        Set<String> violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violatedPaths).contains("shortDescription");
    }

    @Property(tries = 100)
    // Feature: event-management, Property 4: Field constraint enforcement
    void blankCategory(
            @ForAll("blankCategories") String category,
            @ForAll("validTitles") String title,
            @ForAll("validVenueIds") Long venueId,
            @ForAll("futureStartTimes") LocalDateTime startTime) {

        LocalDateTime endTime = startTime.plusHours(2);

        EventRequest request = new EventRequest(
                title,
                "A valid description",
                "A valid short description",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);
        Set<String> violatedPaths = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violatedPaths).contains("category");
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> tooShortTitles() {
        // Titles with length 1 or 2 (less than 3)
        return Arbitraries.oneOf(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(1),
                Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(2)
        );
    }

    @Provide
    Arbitrary<String> tooLongTitles() {
        // Titles exceeding 200 characters
        return Arbitraries.strings().alpha().ofMinLength(201).ofMaxLength(300);
    }

    @Provide
    Arbitrary<String> tooLongDescriptions() {
        // Descriptions exceeding 10000 characters
        return Arbitraries.strings().alpha().ofMinLength(10001).ofMaxLength(10500);
    }

    @Provide
    Arbitrary<String> tooLongShortDescriptions() {
        // Short descriptions exceeding 500 characters
        return Arbitraries.strings().alpha().ofMinLength(501).ofMaxLength(600);
    }

    @Provide
    Arbitrary<String> blankCategories() {
        // Blank or whitespace-only strings
        return Arbitraries.oneOf(
                Arbitraries.of("", " ", "  ", "   ", "\t", "\n", " \t \n "),
                Arbitraries.integers().between(1, 10).map(n -> " ".repeat(n))
        );
    }

    @Provide
    Arbitrary<String> validTitles() {
        return Arbitraries.oneOf(
                Arbitraries.of(
                        "Summer Festival",
                        "Tech Conference 2025",
                        "Comedy Night",
                        "Rock Concert Live",
                        "Annual Charity Gala"
                ),
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
        );
    }

    @Provide
    Arbitrary<String> validCategories() {
        return Arbitraries.of(
                "Music",
                "Technology",
                "Sports",
                "Comedy",
                "Conference",
                "Art",
                "Food"
        );
    }

    @Provide
    Arbitrary<Long> validVenueIds() {
        return Arbitraries.longs().between(1L, 1000L);
    }

    @Provide
    Arbitrary<LocalDateTime> futureStartTimes() {
        LocalDateTime now = LocalDateTime.now();
        return Arbitraries.integers().between(1, 730).flatMap(daysAhead ->
                Arbitraries.integers().between(0, 23).flatMap(hour ->
                        Arbitraries.integers().between(0, 59).map(minute ->
                                now.plusDays(daysAhead).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        )
                )
        );
    }
}
