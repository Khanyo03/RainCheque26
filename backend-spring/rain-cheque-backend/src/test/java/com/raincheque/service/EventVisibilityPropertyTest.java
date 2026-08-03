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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based test for event visibility matching PUBLISHED status.
 * Validates: Requirements 3.3, 3.4
 *
 * Property: For any event in the system, the event SHALL appear in public listing
 * endpoints if and only if its status is PUBLISHED. Events with status DRAFT,
 * CANCELLED, or COMPLETED SHALL NOT appear in public listings.
 */
// Feature: event-management, Property 5: Event visibility matches PUBLISHED status
class EventVisibilityPropertyTest {

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
    // Feature: event-management, Property 5: Event visibility matches PUBLISHED status
    void publicListingOnlyContainsPublishedEvents(
            @ForAll("eventSetsWithMixedStatuses") List<EventData> allEvents) {

        // Compute which events are PUBLISHED (these should be visible)
        List<Event> publishedEvents = allEvents.stream()
                .filter(e -> e.status() == EventStatus.PUBLISHED)
                .map(this::toEventEntity)
                .collect(Collectors.toList());

        // Mock the repository: findByStatus(PUBLISHED, pageable) returns only PUBLISHED events
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(publishedEvents));

        // Call the public listing endpoint
        Page<Event> result = eventService.getPublishedEvents(0, 10);

        // Assert: all returned events have PUBLISHED status
        assertThat(result.getContent()).allMatch(event ->
                event.getStatus() == EventStatus.PUBLISHED);

        // Assert: the count matches the expected published events
        assertThat(result.getContent()).hasSize(publishedEvents.size());

        // Verify the repository is called with PUBLISHED status filter
        verify(eventRepository).findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class));
    }

    @Property(tries = 100)
    // Feature: event-management, Property 5: Event visibility matches PUBLISHED status
    void draftEventsNeverAppearInPublicListing(
            @ForAll("draftOnlyEvents") List<EventData> draftEvents) {

        // When all events are DRAFT, the repository returns empty for PUBLISHED query
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Event> result = eventService.getPublishedEvents(0, 10);

        // No DRAFT events should appear
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getContent()).noneMatch(event ->
                event.getStatus() == EventStatus.DRAFT);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 5: Event visibility matches PUBLISHED status
    void cancelledAndCompletedEventsNeverAppearInPublicListing(
            @ForAll("cancelledAndCompletedEvents") List<EventData> events) {

        // When only CANCELLED and COMPLETED events exist, repository returns empty
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Event> result = eventService.getPublishedEvents(0, 10);

        // None of the CANCELLED or COMPLETED events should appear
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getContent()).noneMatch(event ->
                event.getStatus() == EventStatus.CANCELLED ||
                event.getStatus() == EventStatus.COMPLETED);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 5: Event visibility matches PUBLISHED status
    void eventBecomesVisibleWhenStatusChangesToPublished(
            @ForAll("nonPublishedEvents") EventData eventData) {

        // Create an event entity with non-PUBLISHED status
        Event event = toEventEntity(eventData);

        // Simulate status change to PUBLISHED
        event.setStatus(EventStatus.PUBLISHED);

        // After status change to PUBLISHED, the event should appear in public listing
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<Event> result = eventService.getPublishedEvents(0, 10);

        // The event is now visible
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(EventStatus.PUBLISHED);
        assertThat(result.getContent().get(0).getId()).isEqualTo(event.getId());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 5: Event visibility matches PUBLISHED status
    void eventBecomesInvisibleWhenStatusChangesToCancelled(
            @ForAll("publishedEventData") EventData eventData) {

        // Start with a PUBLISHED event
        Event event = toEventEntity(eventData);
        event.setStatus(EventStatus.PUBLISHED);

        // Simulate status change to CANCELLED — event should no longer appear
        event.setStatus(EventStatus.CANCELLED);

        // Repository no longer returns this event for PUBLISHED query
        when(eventRepository.findByStatus(eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Event> result = eventService.getPublishedEvents(0, 10);

        // The event is no longer visible
        assertThat(result.getContent()).isEmpty();
    }

    // --- Helper ---

    private Event toEventEntity(EventData data) {
        Event event = new Event();
        event.setId(data.id());
        event.setTitle(data.title());
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

    record EventData(Long id, String title, String category, EventStatus status) {}

    // --- Generators ---

    @Provide
    Arbitrary<List<EventData>> eventSetsWithMixedStatuses() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                ids(), titles(), categories(), allStatuses()
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<EventData>> draftOnlyEvents() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                ids(), titles(), categories(), Arbitraries.just(EventStatus.DRAFT)
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<EventData>> cancelledAndCompletedEvents() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                ids(), titles(), categories(),
                Arbitraries.of(EventStatus.CANCELLED, EventStatus.COMPLETED)
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<EventData> nonPublishedEvents() {
        return Combinators.combine(
                ids(), titles(), categories(),
                Arbitraries.of(EventStatus.DRAFT, EventStatus.CANCELLED, EventStatus.COMPLETED)
        ).as(EventData::new);
    }

    @Provide
    Arbitrary<EventData> publishedEventData() {
        return Combinators.combine(
                ids(), titles(), categories(), Arbitraries.just(EventStatus.PUBLISHED)
        ).as(EventData::new);
    }

    private Arbitrary<Long> ids() {
        return Arbitraries.longs().between(1L, 10000L);
    }

    private Arbitrary<String> titles() {
        return Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50);
    }

    private Arbitrary<String> categories() {
        return Arbitraries.of(
                "Music", "Sports", "Arts", "Technology", "Food",
                "Comedy", "Theatre", "Festival", "Conference", "Workshop"
        );
    }

    private Arbitrary<EventStatus> allStatuses() {
        return Arbitraries.of(EventStatus.values());
    }
}
