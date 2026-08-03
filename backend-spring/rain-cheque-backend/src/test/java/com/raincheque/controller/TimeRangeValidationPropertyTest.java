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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for time range validation.
 * Validates: Requirements 1.5, 9.5
 *
 * For any event request where endTime is less than or equal to startTime,
 * the validator SHALL reject the request regardless of all other field values.
 */
// Feature: event-management, Property 3: Time range validation
class TimeRangeValidationPropertyTest {

    private Validator validator;

    @BeforeProperty
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Property(tries = 100)
    // Feature: event-management, Property 3: Time range validation
    void endTimeEqualToStartTimeAlwaysRejected(
            @ForAll("futureStartTimes") LocalDateTime startTime,
            @ForAll("validTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId) {

        // endTime == startTime
        LocalDateTime endTime = startTime;

        EventRequest request = new EventRequest(
                title,
                "A valid description for the event",
                "Short desc",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("End time must be after start time"));
    }

    @Property(tries = 100)
    // Feature: event-management, Property 3: Time range validation
    void endTimeBeforeStartTimeAlwaysRejected(
            @ForAll("futureStartTimes") LocalDateTime startTime,
            @ForAll("minutesBefore") int minutesBefore,
            @ForAll("validTitles") String title,
            @ForAll("validCategories") String category,
            @ForAll("validVenueIds") Long venueId) {

        // endTime < startTime
        LocalDateTime endTime = startTime.minusMinutes(minutesBefore);

        EventRequest request = new EventRequest(
                title,
                "A valid description for the event",
                "Short desc",
                venueId,
                startTime,
                endTime,
                category,
                false,
                null
        );

        Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.getMessage().contains("End time must be after start time"));
    }

    // --- Generators ---

    @Provide
    Arbitrary<LocalDateTime> futureStartTimes() {
        // Generate future start times (1 day to 2 years from now)
        LocalDateTime now = LocalDateTime.now();
        return Arbitraries.integers().between(1, 730).flatMap(daysAhead ->
                Arbitraries.integers().between(0, 23).flatMap(hour ->
                        Arbitraries.integers().between(0, 59).map(minute ->
                                now.plusDays(daysAhead).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                        )
                )
        );
    }

    @Provide
    Arbitrary<Integer> minutesBefore() {
        // Generate a positive number of minutes to subtract (1 minute to 7 days)
        return Arbitraries.integers().between(1, 10080);
    }

    @Provide
    Arbitrary<String> validTitles() {
        // Titles between 3 and 200 characters
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
}
