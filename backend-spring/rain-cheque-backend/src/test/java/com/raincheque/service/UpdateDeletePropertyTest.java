package com.raincheque.service;

import com.raincheque.dto.EventRequest;
import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.entity.Venue;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for update round-trip and delete removal.
 *
 * Property 7: For any valid update request applied to an existing event, reading that event back
 * SHALL return field values matching the update request for all mutable fields.
 * Validates: Requirements 3.1
 *
 * Property 8: For any existing event, after a successful delete operation, querying for that event
 * by ID SHALL produce a 404 response (ResourceNotFoundException).
 * Validates: Requirements 4.1
 */
// Feature: event-management, Property 7: Update round-trip preserves data
// Feature: event-management, Property 8: Delete removes event from existence
class UpdateDeletePropertyTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private VenueRepository venueRepository;

    @BeforeTry
    void setUp() {
        eventRepository = mock(EventRepository.class);
        venueRepository = mock(VenueRepository.class);
        eventService = new EventService(eventRepository, venueRepository);
    }

    // ==========================================
    // Property 7: Update round-trip preserves data
    // ==========================================

    @Property(tries = 100)
    // Feature: event-management, Property 7: Update round-trip preserves data
    // **Validates: Requirements 3.1**
    void updateRoundTripPreservesAllMutableFields(
            @ForAll("existingEvents") Event existingEvent,
            @ForAll("validUpdateRequests") EventRequest updateRequest) {

        Long eventId = existingEvent.getId();

        // Mock venueRepository to return a venue matching the update request's venueId
        Venue updatedVenue = new Venue();
        updatedVenue.setId(updateRequest.venueId());
        updatedVenue.setName("Updated Venue");
        updatedVenue.setAddress("456 Updated St");
        updatedVenue.setCity("Updated City");
        updatedVenue.setCountry("Updated Country");
        updatedVenue.setCapacity(2000);
        when(venueRepository.findById(updateRequest.venueId())).thenReturn(Optional.of(updatedVenue));

        // Mock eventRepository.findById() to return the existing event
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));

        // Mock eventRepository.save() to capture and return the event
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Perform the update
        EventResponse response = eventService.updateEvent(eventId, updateRequest);

        // Verify all mutable fields in the response match the update request
        assertThat(response.getTitle()).isEqualTo(updateRequest.title());
        assertThat(response.getDescription()).isEqualTo(updateRequest.description());
        assertThat(response.getShortDescription()).isEqualTo(updateRequest.shortDescription());
        assertThat(response.getStartTime()).isEqualTo(updateRequest.startTime());
        assertThat(response.getEndTime()).isEqualTo(updateRequest.endTime());
        assertThat(response.getCategory()).isEqualTo(updateRequest.category());
        assertThat(response.isFeatured()).isEqualTo(
                updateRequest.featured() != null ? updateRequest.featured() : false);
        assertThat(response.getVenueName()).isEqualTo(updatedVenue.getName());
    }

    // ==========================================
    // Property 8: Delete removes event from existence
    // ==========================================

    @Property(tries = 100)
    // Feature: event-management, Property 8: Delete removes event from existence
    // **Validates: Requirements 4.1**
    void deleteRemovesEventFromExistence(
            @ForAll("existingEvents") Event existingEvent) {

        Long eventId = existingEvent.getId();

        // Mock eventRepository.findById() to return the existing event initially
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(existingEvent));
        doNothing().when(eventRepository).delete(existingEvent);

        // Perform the delete
        eventService.deleteEvent(eventId);

        // Verify eventRepository.delete() was called with the event
        verify(eventRepository).delete(existingEvent);

        // Now mock findById() to return empty (event no longer exists)
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        // Verify that querying for the event by ID throws ResourceNotFoundException
        assertThatThrownBy(() -> eventService.getEventById(eventId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Generators ---

    @Provide
    Arbitrary<Event> existingEvents() {
        Arbitrary<Long> ids = Arbitraries.longs().between(1L, 10000L);
        Arbitrary<String> titles = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50);
        Arbitrary<String> descriptions = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(200).injectNull(0.2);
        Arbitrary<String> shortDescriptions = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.2);
        Arbitrary<String> categories = Arbitraries.of(
                "Music", "Sports", "Comedy", "Theatre", "Conference",
                "Festival", "Workshop", "Networking", "Art", "Food");
        Arbitrary<Event.EventStatus> statuses = Arbitraries.of(Event.EventStatus.values());
        Arbitrary<Boolean> featuredFlags = Arbitraries.of(true, false);
        Arbitrary<Long> daysAhead = Arbitraries.longs().between(1L, 365L);
        Arbitrary<Long> durationHours = Arbitraries.longs().between(1L, 48L);

        return Combinators.combine(ids, titles, descriptions, shortDescriptions, categories, statuses, featuredFlags, daysAhead)
                .flatAs((id, title, description, shortDescription, category, status, featured, days) ->
                        durationHours.map(duration -> {
                            Event event = new Event();
                            event.setId(id);
                            event.setTitle(title);
                            event.setDescription(description);
                            event.setShortDescription(shortDescription);
                            event.setCategory(category);
                            event.setStatus(status);
                            event.setFeatured(featured);

                            LocalDateTime startTime = LocalDateTime.now().plusDays(days);
                            event.setStartTime(startTime);
                            event.setEndTime(startTime.plusHours(duration));

                            // Set a venue on the existing event
                            Venue venue = new Venue();
                            venue.setId(1L);
                            venue.setName("Original Venue");
                            venue.setAddress("123 Original St");
                            venue.setCity("Original City");
                            venue.setCountry("Original Country");
                            venue.setCapacity(1000);
                            event.setVenue(venue);

                            return event;
                        })
                );
    }

    @Provide
    Arbitrary<EventRequest> validUpdateRequests() {
        Arbitrary<String> titles = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50);
        Arbitrary<String> descriptions = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(200).injectNull(0.2);
        Arbitrary<String> shortDescriptions = Arbitraries.strings().alpha().ofMinLength(0).ofMaxLength(100).injectNull(0.2);
        Arbitrary<Long> venueIds = Arbitraries.longs().between(1L, 1000L);
        Arbitrary<Long> daysAhead = Arbitraries.longs().between(1L, 365L);
        Arbitrary<Long> durationHours = Arbitraries.longs().between(1L, 48L);
        Arbitrary<String> categories = Arbitraries.of(
                "Music", "Sports", "Comedy", "Theatre", "Conference",
                "Festival", "Workshop", "Networking", "Art", "Food");
        Arbitrary<Boolean> featuredFlags = Arbitraries.of(true, false).injectNull(0.1);

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
