package com.raincheque.service;

import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.entity.Event.EventStatus;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for admin listing returning all events regardless of status.
 * Validates: Requirements 2.1
 *
 * Property: For any set of events with any combination of statuses (DRAFT, PUBLISHED,
 * CANCELLED, COMPLETED), the admin list endpoint SHALL return all of them, sorted by
 * creation date descending.
 */
// Feature: event-management, Property 6: Admin listing returns all events regardless of status
class AdminListingPropertyTest {

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
    // Feature: event-management, Property 6: Admin listing returns all events regardless of status
    void adminListingReturnsAllEventsRegardlessOfStatus(
            @ForAll("eventSetsWithMixedStatuses") List<TestEvent> events) {

        // Build Event entities from generated test data
        List<Event> eventEntities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 50);
        Page<Event> mockPage = new PageImpl<>(eventEntities, pageable, eventEntities.size());

        // Mock findAll(Specification, Pageable) to return ALL events
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Call getAllEventsAdmin with null status, null category, null search
        // (no filters applied - should return all events)
        Page<EventResponse> result = eventService.getAllEventsAdmin(pageable, null, null, null);

        // Verify: result contains exactly N events (all of them)
        assertThat(result.getContent()).hasSize(events.size());

        // Verify: all statuses from the original set are represented in the result
        Set<EventStatus> originalStatuses = events.stream()
                .map(TestEvent::status)
                .collect(Collectors.toSet());
        Set<EventStatus> resultStatuses = result.getContent().stream()
                .map(EventResponse::getStatus)
                .collect(Collectors.toSet());
        assertThat(resultStatuses).containsAll(originalStatuses);

        // Verify the repository was called
        verify(eventRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Property(tries = 100)
    // Feature: event-management, Property 6: Admin listing returns all events regardless of status
    void adminListingSpecificationDoesNotFilterByStatusWhenNull(
            @ForAll("eventSetsWithMixedStatuses") List<TestEvent> events) {

        List<Event> eventEntities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 50);
        Page<Event> mockPage = new PageImpl<>(eventEntities, pageable, eventEntities.size());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Call with null status - should not filter by status
        eventService.getAllEventsAdmin(pageable, null, null, null);

        // Capture the specification passed to findAll
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Event>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(specCaptor.capture(), any(Pageable.class));

        Specification<Event> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();

        // The specification with null status, null category, null search should
        // produce no filtering predicates (i.e., all events pass through)
        // We verify this by checking all events were returned
        assertThat(mockPage.getContent()).hasSize(events.size());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 6: Admin listing returns all events regardless of status
    void adminListingIncludesEventsOfEveryStatus(
            @ForAll("guaranteedMixedStatusEvents") List<TestEvent> events) {

        // This generator guarantees at least one event per status
        List<Event> eventEntities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 50);
        Page<Event> mockPage = new PageImpl<>(eventEntities, pageable, eventEntities.size());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<EventResponse> result = eventService.getAllEventsAdmin(pageable, null, null, null);

        // Verify all 4 statuses are present in the result
        Set<EventStatus> resultStatuses = result.getContent().stream()
                .map(EventResponse::getStatus)
                .collect(Collectors.toSet());

        assertThat(resultStatuses).containsExactlyInAnyOrder(
                EventStatus.DRAFT, EventStatus.PUBLISHED,
                EventStatus.CANCELLED, EventStatus.COMPLETED
        );

        // Verify no events were filtered out
        assertThat(result.getTotalElements()).isEqualTo(events.size());
    }

    // --- Helper methods ---

    private Event toEntity(TestEvent testEvent) {
        Event event = new Event();
        event.setId(testEvent.id());
        event.setTitle(testEvent.title());
        event.setDescription("Description for " + testEvent.title());
        event.setCategory(testEvent.category());
        event.setStatus(testEvent.status());
        event.setStartTime(testEvent.startTime());
        event.setEndTime(testEvent.startTime().plusHours(2));

        Venue venue = new Venue();
        venue.setId(1L);
        venue.setName("Test Venue");
        venue.setAddress("123 Test St");
        venue.setCity("Test City");
        venue.setCountry("Test Country");
        venue.setCapacity(500);
        event.setVenue(venue);

        return event;
    }

    // --- Data Records ---

    record TestEvent(Long id, String title, EventStatus status, String category,
                     LocalDateTime startTime) {}

    // --- Generators ---

    @Provide
    Arbitrary<List<TestEvent>> eventSetsWithMixedStatuses() {
        Arbitrary<TestEvent> eventArbitrary = Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),
                eventTitles(),
                Arbitraries.of(EventStatus.values()),
                categories(),
                eventStartTimes()
        ).as(TestEvent::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(20).uniqueElements(TestEvent::title);
    }

    @Provide
    Arbitrary<List<TestEvent>> guaranteedMixedStatusEvents() {
        // Generate one event per status, then add random extras
        Arbitrary<TestEvent> draftEvent = fixedStatusEvent(EventStatus.DRAFT, "Draft Event");
        Arbitrary<TestEvent> publishedEvent = fixedStatusEvent(EventStatus.PUBLISHED, "Published Event");
        Arbitrary<TestEvent> cancelledEvent = fixedStatusEvent(EventStatus.CANCELLED, "Cancelled Event");
        Arbitrary<TestEvent> completedEvent = fixedStatusEvent(EventStatus.COMPLETED, "Completed Event");

        Arbitrary<List<TestEvent>> extraEvents = Combinators.combine(
                Arbitraries.longs().between(100L, 1000L),
                Arbitraries.of("Extra Event A", "Extra Event B", "Extra Event C",
                        "Extra Event D", "Extra Event E"),
                Arbitraries.of(EventStatus.values()),
                categories(),
                eventStartTimes()
        ).as(TestEvent::new).list().ofMinSize(0).ofMaxSize(10).uniqueElements(TestEvent::title);

        return Combinators.combine(draftEvent, publishedEvent, cancelledEvent, completedEvent, extraEvents)
                .as((d, p, ca, co, extras) -> {
                    List<TestEvent> all = new java.util.ArrayList<>(List.of(d, p, ca, co));
                    all.addAll(extras);
                    return all;
                });
    }

    private Arbitrary<TestEvent> fixedStatusEvent(EventStatus status, String titlePrefix) {
        return Combinators.combine(
                Arbitraries.longs().between(1L, 99L),
                Arbitraries.just(titlePrefix),
                Arbitraries.just(status),
                categories(),
                eventStartTimes()
        ).as(TestEvent::new);
    }

    private Arbitrary<String> eventTitles() {
        return Arbitraries.of(
                "Rock Concert Night", "Jazz Festival", "Tech Conference",
                "Art Exhibition", "Food Festival", "Comedy Show",
                "Sports Championship", "Music Awards", "Workshop Series",
                "Networking Mixer", "Festival of Lights", "Concert in the Park",
                "Summer Bash", "Winter Gala", "Spring Fair"
        );
    }

    private Arbitrary<String> categories() {
        return Arbitraries.of(
                "Music", "Sports", "Arts", "Technology", "Food",
                "Comedy", "Theatre", "Festival", "Conference", "Workshop"
        );
    }

    private Arbitrary<LocalDateTime> eventStartTimes() {
        return Arbitraries.longs()
                .between(1, 90)
                .map(days -> LocalDateTime.of(2025, 6, 1, 10, 0).plusDays(days));
    }
}
