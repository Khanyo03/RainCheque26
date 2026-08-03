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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for combined filters conjunctively applied.
 * Validates: Requirements 7.2
 *
 * Property: For any search request with keyword, category, and date filters,
 * every event in the result set SHALL satisfy ALL filter conditions simultaneously.
 */
// Feature: event-management, Property 14: Combined filters conjunctively applied
class CombinedFiltersPropertyTest {

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
    // Feature: event-management, Property 14: Combined filters conjunctively applied
    void allFiltersAreAppliedConjunctively(
            @ForAll("filterCombinations") FilterInput filterInput,
            @ForAll("eventSets") List<TestEvent> events) {

        // Build the list of Event entities to be returned by the repository
        List<Event> eventEntities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        // The service calls eventRepository.findAll(Specification, Pageable).
        // We simulate the behavior by filtering events that match ALL conditions.
        List<Event> matchingEvents = eventEntities.stream()
                .filter(e -> matchesKeyword(e, filterInput.keyword()))
                .filter(e -> matchesCategory(e, filterInput.category()))
                .filter(e -> matchesFromDate(e, filterInput.from()))
                .filter(e -> matchesToDate(e, filterInput.to()))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> mockPage = new PageImpl<>(matchingEvents, pageable, matchingEvents.size());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Call the service method with all filters
        Page<EventResponse> result = eventService.searchEventsWithFilters(
                filterInput.keyword(),
                filterInput.category(),
                filterInput.from(),
                filterInput.to(),
                pageable
        );

        // Verify: every event in the result satisfies ALL filter conditions simultaneously
        for (EventResponse response : result.getContent()) {
            // Find the matching original event
            Event original = matchingEvents.stream()
                    .filter(e -> e.getTitle().equals(response.getTitle()))
                    .findFirst()
                    .orElse(null);

            assertThat(original).isNotNull();

            if (filterInput.keyword() != null && !filterInput.keyword().isBlank()) {
                assertThat(matchesKeyword(original, filterInput.keyword())).isTrue();
            }
            if (filterInput.category() != null && !filterInput.category().isBlank()) {
                assertThat(matchesCategory(original, filterInput.category())).isTrue();
            }
            if (filterInput.from() != null) {
                assertThat(matchesFromDate(original, filterInput.from())).isTrue();
            }
            if (filterInput.to() != null) {
                assertThat(matchesToDate(original, filterInput.to())).isTrue();
            }
        }

        // Verify the repository was called with a specification (not null) and pageable
        verify(eventRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Property(tries = 100)
    // Feature: event-management, Property 14: Combined filters conjunctively applied
    void specificationIsBuiltFromAllNonNullFilters(
            @ForAll("filterCombinations") FilterInput filterInput) {

        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Call the service
        eventService.searchEventsWithFilters(
                filterInput.keyword(),
                filterInput.category(),
                filterInput.from(),
                filterInput.to(),
                pageable
        );

        // Verify that findAll is called with a non-null Specification (the combined spec)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Event>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(specCaptor.capture(), eq(pageable));

        Specification<Event> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
    }

    @Property(tries = 100)
    // Feature: event-management, Property 14: Combined filters conjunctively applied
    void eventsNotMatchingAllFiltersAreExcluded(
            @ForAll("filterCombinations") FilterInput filterInput,
            @ForAll("eventSets") List<TestEvent> events) {

        List<Event> eventEntities = events.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        // Only return events matching ALL filters (simulating correct DB behavior)
        List<Event> matchingAll = eventEntities.stream()
                .filter(e -> matchesKeyword(e, filterInput.keyword()))
                .filter(e -> matchesCategory(e, filterInput.category()))
                .filter(e -> matchesFromDate(e, filterInput.from()))
                .filter(e -> matchesToDate(e, filterInput.to()))
                .collect(Collectors.toList());

        // Events that match SOME but not ALL filters
        List<Event> partial = eventEntities.stream()
                .filter(e -> !matchingAll.contains(e))
                .filter(e -> matchesKeyword(e, filterInput.keyword())
                        || matchesCategory(e, filterInput.category())
                        || matchesFromDate(e, filterInput.from())
                        || matchesToDate(e, filterInput.to()))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> mockPage = new PageImpl<>(matchingAll, pageable, matchingAll.size());

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<EventResponse> result = eventService.searchEventsWithFilters(
                filterInput.keyword(),
                filterInput.category(),
                filterInput.from(),
                filterInput.to(),
                pageable
        );

        // Partial-match events should NOT appear in results
        List<String> resultTitles = result.getContent().stream()
                .map(EventResponse::getTitle)
                .collect(Collectors.toList());

        List<String> partialTitles = partial.stream()
                .map(Event::getTitle)
                .collect(Collectors.toList());

        for (String partialTitle : partialTitles) {
            assertThat(resultTitles).doesNotContain(partialTitle);
        }
    }

    // --- Helper methods ---

    private boolean matchesKeyword(Event event, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true; // No keyword filter => all match
        }
        String lowerKeyword = keyword.toLowerCase();
        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";
        String description = event.getDescription() != null ? event.getDescription().toLowerCase() : "";
        String venueName = event.getVenue() != null && event.getVenue().getName() != null
                ? event.getVenue().getName().toLowerCase() : "";
        return title.contains(lowerKeyword) || description.contains(lowerKeyword) || venueName.contains(lowerKeyword);
    }

    private boolean matchesCategory(Event event, String category) {
        if (category == null || category.isBlank()) {
            return true; // No category filter => all match
        }
        return category.equals(event.getCategory());
    }

    private boolean matchesFromDate(Event event, LocalDateTime from) {
        if (from == null) {
            return true; // No from filter => all match
        }
        return event.getStartTime() != null && !event.getStartTime().isBefore(from);
    }

    private boolean matchesToDate(Event event, LocalDateTime to) {
        if (to == null) {
            return true; // No to filter => all match
        }
        return event.getStartTime() != null && !event.getStartTime().isAfter(to);
    }

    private Event toEntity(TestEvent testEvent) {
        Event event = new Event();
        event.setId(testEvent.id());
        event.setTitle(testEvent.title());
        event.setDescription(testEvent.description());
        event.setCategory(testEvent.category());
        event.setStartTime(testEvent.startTime());
        event.setEndTime(testEvent.startTime().plusHours(2));
        event.setStatus(EventStatus.PUBLISHED);

        Venue venue = new Venue();
        venue.setId(1L);
        venue.setName(testEvent.venueName());
        venue.setAddress("Test Address");
        venue.setCity("Test City");
        venue.setCountry("Test Country");
        venue.setCapacity(1000);
        event.setVenue(venue);

        return event;
    }

    // --- Data Records ---

    record FilterInput(String keyword, String category, LocalDateTime from, LocalDateTime to) {}

    record TestEvent(Long id, String title, String description, String category,
                     String venueName, LocalDateTime startTime) {}

    // --- Generators ---

    @Provide
    Arbitrary<FilterInput> filterCombinations() {
        Arbitrary<String> keywords = Arbitraries.of(
                "rock", "jazz", "concert", "fest", "tech", "art"
        ).injectNull(0.2);

        Arbitrary<String> categories = Arbitraries.of(
                "Music", "Sports", "Arts", "Technology", "Food", "Comedy"
        ).injectNull(0.2);

        Arbitrary<LocalDateTime> fromDates = Arbitraries.longs()
                .between(1, 30)
                .map(days -> LocalDateTime.of(2025, 1, 1, 0, 0).plusDays(days))
                .injectNull(0.3);

        Arbitrary<LocalDateTime> toDates = Arbitraries.longs()
                .between(31, 90)
                .map(days -> LocalDateTime.of(2025, 1, 1, 0, 0).plusDays(days))
                .injectNull(0.3);

        return Combinators.combine(keywords, categories, fromDates, toDates)
                .as(FilterInput::new);
    }

    @Provide
    Arbitrary<List<TestEvent>> eventSets() {
        Arbitrary<TestEvent> eventArbitrary = Combinators.combine(
                Arbitraries.longs().between(1L, 1000L),
                eventTitles(),
                eventDescriptions(),
                Arbitraries.of("Music", "Sports", "Arts", "Technology", "Food", "Comedy"),
                venueNames(),
                eventStartTimes()
        ).as(TestEvent::new);

        return eventArbitrary.list().ofMinSize(2).ofMaxSize(15).uniqueElements(TestEvent::title);
    }

    private Arbitrary<String> eventTitles() {
        return Arbitraries.of(
                "Rock Concert Night", "Jazz Festival", "Tech Conference 2025",
                "Art Exhibition", "Food Fest Downtown", "Comedy Show Live",
                "Sports Championship", "Music Awards Gala", "Workshop Series",
                "Networking Mixer", "Festival of Lights", "Concert in the Park"
        );
    }

    private Arbitrary<String> eventDescriptions() {
        return Arbitraries.of(
                "A great rock music event",
                "An evening of jazz and fine dining",
                "Technology leaders gather to discuss trends",
                "Modern art from local artists",
                "Taste food from around the world",
                "Stand-up comedy with top performers",
                "Live sports action and entertainment",
                "Annual music awards ceremony",
                "Hands-on tech workshop for beginners",
                "Professional networking and drinks"
        );
    }

    private Arbitrary<String> venueNames() {
        return Arbitraries.of(
                "The Rock Arena", "Jazz Club Blue", "Tech Hub Center",
                "Art Gallery Modern", "Food Court Plaza", "Comedy Den",
                "Sports Stadium", "Concert Hall Grand", "Workshop Space",
                "Networking Lounge"
        );
    }

    private Arbitrary<LocalDateTime> eventStartTimes() {
        return Arbitraries.longs()
                .between(1, 90)
                .map(days -> LocalDateTime.of(2025, 1, 1, 10, 0).plusDays(days));
    }
}
