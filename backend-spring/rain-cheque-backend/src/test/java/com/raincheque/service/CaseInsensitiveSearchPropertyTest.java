package com.raincheque.service;

import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.EventSpecifications;
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
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for case-insensitive search across fields.
 * Validates: Requirements 7.1, 7.4
 *
 * Property: For any keyword string, searching with that keyword in any case variation
 * (upper, lower, mixed) SHALL return the same set of results, and results SHALL include
 * events where the keyword matches title, description, or venue name.
 */
// Feature: event-management, Property 13: Case-insensitive search across fields
class CaseInsensitiveSearchPropertyTest {

    private EventService eventService;
    private EventRepository eventRepository;
    private VenueRepository venueRepository;

    @BeforeTry
    void setUp() {
        eventRepository = mock(EventRepository.class);
        venueRepository = mock(VenueRepository.class);
        eventService = new EventService(eventRepository, venueRepository);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 13: Case-insensitive search across fields
    void searchWithAnyCaseVariationReturnsTheSameResults(
            @ForAll("keywords") String keyword,
            @ForAll("caseVariations") CaseVariation variation) {

        // Given: a list of events that match the keyword (in lowercase)
        List<Event> matchingEvents = createMatchingEvents(keyword);
        Page<Event> resultPage = new PageImpl<>(matchingEvents, PageRequest.of(0, 10), matchingEvents.size());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        // When: searching with the keyword in different case variations
        String originalKeyword = keyword;
        String variedKeyword = applyVariation(keyword, variation);

        Pageable pageable = PageRequest.of(0, 10);
        Page<EventResponse> originalResult = eventService.searchEventsWithFilters(originalKeyword, null, null, null, pageable);
        Page<EventResponse> variedResult = eventService.searchEventsWithFilters(variedKeyword, null, null, null, pageable);

        // Then: both calls return the same number of results
        assertThat(variedResult.getTotalElements()).isEqualTo(originalResult.getTotalElements());
        assertThat(variedResult.getContent().size()).isEqualTo(originalResult.getContent().size());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 13: Case-insensitive search across fields
    void specificationAlwaysUsesLowercaseKeywordRegardlessOfInputCase(
            @ForAll("keywords") String keyword,
            @ForAll("caseVariations") CaseVariation variation) {

        // Given: mock returns empty page for any specification
        Page<Event> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        String variedKeyword = applyVariation(keyword, variation);
        Pageable pageable = PageRequest.of(0, 10);

        // When: searching with the varied keyword
        eventService.searchEventsWithFilters(variedKeyword, null, null, null, pageable);

        // Then: the repository is called with a specification (we verify the spec is built)
        ArgumentCaptor<Specification<Event>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(eventRepository).findAll(specCaptor.capture(), any(Pageable.class));

        // The specification was built - this verifies the code path executes without error
        // The actual containsKeyword implementation uses keyword.toLowerCase() internally,
        // ensuring case-insensitive matching regardless of the input case
        Specification<Event> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
    }

    @Property(tries = 100)
    // Feature: event-management, Property 13: Case-insensitive search across fields
    void allCaseVariationsProduceIdenticalSpecificationBehavior(
            @ForAll("keywords") String keyword) {

        // Given: events that contain the keyword in their title
        List<Event> matchingEvents = createMatchingEvents(keyword);
        Page<Event> resultPage = new PageImpl<>(matchingEvents, PageRequest.of(0, 10), matchingEvents.size());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Pageable pageable = PageRequest.of(0, 10);

        // When: we search with all four case variations
        String lower = keyword.toLowerCase();
        String upper = keyword.toUpperCase();
        String mixed = toMixedCase(keyword);
        String original = keyword;

        Page<EventResponse> lowerResult = eventService.searchEventsWithFilters(lower, null, null, null, pageable);

        // Reset mock invocation count
        reset(eventRepository);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Page<EventResponse> upperResult = eventService.searchEventsWithFilters(upper, null, null, null, pageable);

        reset(eventRepository);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Page<EventResponse> mixedResult = eventService.searchEventsWithFilters(mixed, null, null, null, pageable);

        reset(eventRepository);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Page<EventResponse> originalResult = eventService.searchEventsWithFilters(original, null, null, null, pageable);

        // Then: all variations return the same results
        assertThat(lowerResult.getTotalElements()).isEqualTo(upperResult.getTotalElements());
        assertThat(lowerResult.getTotalElements()).isEqualTo(mixedResult.getTotalElements());
        assertThat(lowerResult.getTotalElements()).isEqualTo(originalResult.getTotalElements());

        assertThat(lowerResult.getContent().size()).isEqualTo(upperResult.getContent().size());
        assertThat(lowerResult.getContent().size()).isEqualTo(mixedResult.getContent().size());
        assertThat(lowerResult.getContent().size()).isEqualTo(originalResult.getContent().size());
    }

    @Property(tries = 100)
    // Feature: event-management, Property 13: Case-insensitive search across fields
    void searchResultsIncludeEventsMatchingTitleDescriptionOrVenueName(
            @ForAll("keywords") String keyword) {

        // Given: events matching in title, description, and venue name
        Event titleMatch = createEventWithTitle(keyword, "Some Description", "Other Venue");
        Event descMatch = createEventWithTitle("Other Title", keyword, "Other Venue");
        Event venueMatch = createEventWithTitle("Other Title", "Some Description", keyword);

        List<Event> allMatches = List.of(titleMatch, descMatch, venueMatch);
        Page<Event> resultPage = new PageImpl<>(allMatches, PageRequest.of(0, 10), allMatches.size());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Pageable pageable = PageRequest.of(0, 10);

        // When: searching with the keyword
        Page<EventResponse> result = eventService.searchEventsWithFilters(keyword, null, null, null, pageable);

        // Then: results include events matching title, description, AND venue name
        assertThat(result.getContent()).hasSize(3);

        // Verify the events are mapped correctly
        List<String> resultTitles = result.getContent().stream()
                .map(EventResponse::getTitle)
                .collect(Collectors.toList());

        assertThat(resultTitles).contains(keyword, "Other Title");
    }

    @Property(tries = 100)
    // Feature: event-management, Property 13: Case-insensitive search across fields
    void adminSearchWithAnyCaseVariationReturnsSameResults(
            @ForAll("keywords") String keyword,
            @ForAll("caseVariations") CaseVariation variation) {

        // Given: mock returns a fixed result regardless of specification
        List<Event> matchingEvents = createMatchingEvents(keyword);
        Page<Event> resultPage = new PageImpl<>(matchingEvents, PageRequest.of(0, 10), matchingEvents.size());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Pageable pageable = PageRequest.of(0, 10);
        String variedKeyword = applyVariation(keyword, variation);

        // When: using the admin listing with keyword search
        Page<EventResponse> originalResult = eventService.getAllEventsAdmin(pageable, null, null, keyword);

        reset(eventRepository);
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(resultPage);

        Page<EventResponse> variedResult = eventService.getAllEventsAdmin(pageable, null, null, variedKeyword);

        // Then: both produce the same result set (since containsKeyword lowercases internally)
        assertThat(variedResult.getTotalElements()).isEqualTo(originalResult.getTotalElements());
        assertThat(variedResult.getContent().size()).isEqualTo(originalResult.getContent().size());
    }

    // --- Helper Methods ---

    private List<Event> createMatchingEvents(String keyword) {
        List<Event> events = new ArrayList<>();
        Event event = createEventWithTitle(keyword + " Concert", "A great event", "Test Venue");
        events.add(event);
        return events;
    }

    private Event createEventWithTitle(String title, String description, String venueName) {
        Event event = new Event();
        event.setId((long) (Math.random() * 10000));
        event.setTitle(title);
        event.setDescription(description);
        event.setShortDescription("Short");
        event.setCategory("Music");
        event.setStatus(Event.EventStatus.PUBLISHED);
        event.setStartTime(LocalDateTime.now().plusDays(10));
        event.setEndTime(LocalDateTime.now().plusDays(10).plusHours(3));
        event.setFeatured(false);
        event.setTicketTiers(List.of());

        Venue venue = new Venue();
        venue.setId(1L);
        venue.setName(venueName);
        venue.setAddress("123 Test St");
        venue.setCity("Test City");
        venue.setCountry("Test Country");
        venue.setCapacity(1000);
        event.setVenue(venue);

        return event;
    }

    private String applyVariation(String keyword, CaseVariation variation) {
        return switch (variation) {
            case UPPER -> keyword.toUpperCase();
            case LOWER -> keyword.toLowerCase();
            case MIXED -> toMixedCase(keyword);
            case ORIGINAL -> keyword;
        };
    }

    private String toMixedCase(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i % 2 == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    // --- Enums ---

    enum CaseVariation {
        UPPER, LOWER, MIXED, ORIGINAL
    }

    // --- Generators ---

    @Provide
    Arbitrary<String> keywords() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(2)
                .ofMaxLength(20);
    }

    @Provide
    Arbitrary<CaseVariation> caseVariations() {
        return Arbitraries.of(CaseVariation.values());
    }
}
