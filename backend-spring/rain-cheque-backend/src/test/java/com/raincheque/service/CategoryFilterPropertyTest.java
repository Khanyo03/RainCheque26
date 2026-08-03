package com.raincheque.service;

import com.raincheque.entity.Event;
import com.raincheque.entity.Event.EventStatus;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for category filter returning only matching published events.
 * Validates: Requirements 6.2
 */
// Feature: event-management, Property 12: Category filter returns only matching published events
class CategoryFilterPropertyTest {

    private EventRepository eventRepository;
    private VenueRepository venueRepository;
    private EventService eventService;

    @BeforeTry
    void setUp() {
        eventRepository = mock(EventRepository.class);
        venueRepository = mock(VenueRepository.class);
        eventService = new EventService(eventRepository, venueRepository);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 12: Category filter returns only matching published events
    void allReturnedEventsHaveRequestedCategoryAndPublishedStatus(
            @ForAll("categoryValues") String category,
            @ForAll("pageNumbers") int page,
            @ForAll("pageSizes") int size) {

        // Build events that match the category AND are PUBLISHED (simulating what the repository returns)
        List<Event> matchingEvents = buildMatchingEvents(category, size);

        // Mock repository to return only matching events (as the real query would)
        when(eventRepository.findByCategoryAndStatus(
                eq(category),
                eq(EventStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(matchingEvents));

        // Call the service method under test
        Page<Event> result = eventService.getEventsByCategory(category, page, size);

        // Verify: ALL returned events have the exact requested category
        assertThat(result.getContent()).allSatisfy(event ->
                assertThat(event.getCategory()).isEqualTo(category));

        // Verify: ALL returned events have PUBLISHED status
        assertThat(result.getContent()).allSatisfy(event ->
                assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED));

        // Verify: the repository was called with correct parameters
        verify(eventRepository).findByCategoryAndStatus(
                eq(category),
                eq(EventStatus.PUBLISHED),
                any(Pageable.class));
    }

    @Property(tries = 100)
    // Feature: event-management, Property 12: Category filter returns only matching published events
    void noEventsWithDifferentCategoryOrNonPublishedStatusLeakThrough(
            @ForAll("categoryValues") String requestedCategory,
            @ForAll("mixedEventSets") List<EventData> allEvents) {

        // Separate events into matching (correct category AND PUBLISHED)
        List<Event> matchingEvents = allEvents.stream()
                .filter(e -> e.category().equals(requestedCategory) && e.status() == EventStatus.PUBLISHED)
                .map(this::toEvent)
                .collect(Collectors.toList());

        // Mock repository to return only matching events
        when(eventRepository.findByCategoryAndStatus(
                eq(requestedCategory),
                eq(EventStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(matchingEvents));

        // Call the service
        Page<Event> result = eventService.getEventsByCategory(requestedCategory, 0, 10);

        // Verify: no events with a different category appear in results
        assertThat(result.getContent()).allSatisfy(event ->
                assertThat(event.getCategory()).isEqualTo(requestedCategory));

        // Verify: no non-PUBLISHED events appear in results
        assertThat(result.getContent()).allSatisfy(event ->
                assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED));

        // Verify: count of returned events matches only those with correct category + PUBLISHED
        long expectedCount = allEvents.stream()
                .filter(e -> e.category().equals(requestedCategory) && e.status() == EventStatus.PUBLISHED)
                .count();
        assertThat(result.getContent()).hasSize((int) expectedCount);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 12: Category filter returns only matching published events
    void filterPassesExactCategoryToRepository(
            @ForAll("categoryValues") String category) {

        // Mock empty result
        when(eventRepository.findByCategoryAndStatus(
                eq(category),
                eq(EventStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Call service
        eventService.getEventsByCategory(category, 0, 10);

        // Verify the exact category string is passed to repository (no transformation)
        verify(eventRepository).findByCategoryAndStatus(
                eq(category),
                eq(EventStatus.PUBLISHED),
                any(Pageable.class));
    }

    // --- Helper methods ---

    private List<Event> buildMatchingEvents(String category, int count) {
        List<Event> events = new ArrayList<>();
        int eventCount = Math.min(count, 5); // cap to avoid excessive list sizes
        for (int i = 0; i < eventCount; i++) {
            Event event = new Event();
            event.setId((long) (i + 1));
            event.setTitle("Event " + (i + 1));
            event.setCategory(category);
            event.setStatus(EventStatus.PUBLISHED);
            event.setStartTime(LocalDateTime.now().plusDays(i + 1));
            event.setEndTime(LocalDateTime.now().plusDays(i + 1).plusHours(2));
            Venue venue = new Venue();
            venue.setId(1L);
            venue.setName("Test Venue");
            event.setVenue(venue);
            events.add(event);
        }
        return events;
    }

    private Event toEvent(EventData data) {
        Event event = new Event();
        event.setId(data.id());
        event.setTitle("Event " + data.id());
        event.setCategory(data.category());
        event.setStatus(data.status());
        event.setStartTime(LocalDateTime.now().plusDays(1));
        event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
        Venue venue = new Venue();
        venue.setId(1L);
        venue.setName("Test Venue");
        event.setVenue(venue);
        return event;
    }

    // --- Data Records ---

    record EventData(Long id, String category, EventStatus status) {}

    // --- Generators ---

    @Provide
    Arbitrary<String> categoryValues() {
        return Arbitraries.of(
                "Music", "Sports", "Arts", "Technology", "Food",
                "Comedy", "Theatre", "Festival", "Conference", "Workshop"
        );
    }

    @Provide
    Arbitrary<Integer> pageNumbers() {
        return Arbitraries.integers().between(0, 10);
    }

    @Provide
    Arbitrary<Integer> pageSizes() {
        return Arbitraries.integers().between(1, 20);
    }

    @Provide
    Arbitrary<List<EventData>> mixedEventSets() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                Arbitraries.longs().between(1L, 100L),
                categoryValues(),
                Arbitraries.of(EventStatus.values())
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }
}
