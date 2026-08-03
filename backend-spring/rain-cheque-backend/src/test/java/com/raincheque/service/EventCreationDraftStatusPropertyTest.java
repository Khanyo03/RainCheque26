package com.raincheque.service;

import com.raincheque.dto.EventRequest;
import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based test for event creation always producing DRAFT status.
 * Validates: Requirements 1.1
 *
 * Property: For any valid event creation request submitted by an authorized user,
 * the persisted event SHALL have status DRAFT regardless of any other input values.
 */
// Feature: event-management, Property 1: Event creation always produces DRAFT status
class EventCreationDraftStatusPropertyTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private VenueRepository venueRepository;

    @BeforeTry
    void setUp() {
        eventRepository = mock(EventRepository.class);
        venueRepository = mock(VenueRepository.class);
        eventService = new EventService(eventRepository, venueRepository);

        // VenueRepository.findById() returns a mock Venue for any ID
        Venue mockVenue = new Venue();
        mockVenue.setId(1L);
        mockVenue.setName("Test Venue");
        mockVenue.setAddress("123 Test St");
        mockVenue.setCity("Test City");
        mockVenue.setCountry("Test Country");
        mockVenue.setCapacity(1000);
        when(venueRepository.findById(anyLong())).thenReturn(Optional.of(mockVenue));

        // EventRepository.save() returns the event passed to it (capture and return)
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(1L);
            return event;
        });
    }

    @Property(tries = 100)
    // Feature: event-management, Property 1: Event creation always produces DRAFT status
    void createdEventAlwaysHasDraftStatus(
            @ForAll("validEventRequests") EventRequest request) {

        EventResponse response = eventService.createEvent(request);

        assertThat(response.getStatus()).isEqualTo(Event.EventStatus.DRAFT);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 1: Event creation always produces DRAFT status
    void savedEventEntityAlwaysHasDraftStatus(
            @ForAll("validEventRequests") EventRequest request) {

        eventService.createEvent(request);

        // Verify the event saved to the repository has DRAFT status
        verify(eventRepository).save(argThat(event ->
                event.getStatus() == Event.EventStatus.DRAFT
        ));
    }

    // --- Generators ---

    @Provide
    Arbitrary<EventRequest> validEventRequests() {
        Arbitrary<String> titles = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(50);

        Arbitrary<String> descriptions = Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(200)
                .injectNull(0.2);

        Arbitrary<String> shortDescriptions = Arbitraries.strings()
                .alpha()
                .ofMinLength(0)
                .ofMaxLength(100)
                .injectNull(0.2);

        Arbitrary<Long> venueIds = Arbitraries.longs().between(1L, 1000L);

        Arbitrary<Long> daysAhead = Arbitraries.longs().between(1L, 365L);

        Arbitrary<Long> durationHours = Arbitraries.longs().between(1L, 48L);

        Arbitrary<String> categories = Arbitraries.of(
                "Music", "Sports", "Comedy", "Theatre", "Conference",
                "Festival", "Workshop", "Networking", "Art", "Food"
        );

        Arbitrary<Boolean> featuredFlags = Arbitraries.of(true, false)
                .injectNull(0.1);

        Arbitrary<Event.EventMetadata> metadataArb = Arbitraries.oneOf(
                Arbitraries.just((Event.EventMetadata) null),
                Combinators.combine(
                        Arbitraries.integers().between(0, 21),
                        Arbitraries.of("standard", "rain-or-shine", "cancel-if-rain"),
                        Arbitraries.of(true, false),
                        Arbitraries.of(true, false)
                ).as((age, weather, parking, food) -> {
                    Event.EventMetadata meta = new Event.EventMetadata();
                    meta.setAgeRestriction(age);
                    meta.setWeatherPolicy(weather);
                    meta.setParkingAvailable(parking);
                    meta.setFoodAllowed(food);
                    return meta;
                })
        );

        return Combinators.combine(titles, descriptions, shortDescriptions, venueIds, daysAhead, durationHours, categories, featuredFlags)
                .flatAs((title, description, shortDescription, venueId, days, duration, category, featured) ->
                        metadataArb.map(metadata -> {
                            LocalDateTime startTime = LocalDateTime.now().plusDays(days);
                            LocalDateTime endTime = startTime.plusHours(duration);
                            return new EventRequest(
                                    title,
                                    description,
                                    shortDescription,
                                    venueId,
                                    startTime,
                                    endTime,
                                    category,
                                    featured,
                                    metadata
                            );
                        })
                );
    }
}
