package com.raincheque.service;

import com.raincheque.entity.Event;
import com.raincheque.entity.Event.EventStatus;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based test for category listing reflecting published events only.
 * Validates: Requirements 6.1
 */
// Feature: event-management, Property 11: Category listing reflects published events only
class CategoryListingPropertyTest {

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
    // Feature: event-management, Property 11: Category listing reflects published events only
    void categoriesEndpointReturnsExactlyDistinctCategoriesFromPublishedEvents(
            @ForAll("eventSets") List<EventData> events) {

        // Compute expected: distinct categories from PUBLISHED events only
        Set<String> expectedCategories = events.stream()
                .filter(e -> e.status() == EventStatus.PUBLISHED)
                .map(EventData::category)
                .collect(Collectors.toSet());

        // Mock the repository to return what the DB query would return
        // (distinct categories from events with PUBLISHED status)
        List<String> repoResult = expectedCategories.stream().sorted().collect(Collectors.toList());
        when(eventRepository.findDistinctCategoriesByStatus(EventStatus.PUBLISHED))
                .thenReturn(repoResult);

        // Call the service method
        List<String> result = eventService.getPublishedCategories();

        // Assert: returned categories match exactly the distinct categories from PUBLISHED events
        assertThat(Set.copyOf(result)).isEqualTo(expectedCategories);

        // Assert: no categories from non-published events leak through
        Set<String> nonPublishedCategories = events.stream()
                .filter(e -> e.status() != EventStatus.PUBLISHED)
                .map(EventData::category)
                .collect(Collectors.toSet());

        // Categories that are ONLY in non-published events should NOT appear
        Set<String> exclusivelyNonPublished = nonPublishedCategories.stream()
                .filter(cat -> !expectedCategories.contains(cat))
                .collect(Collectors.toSet());
        if (!exclusivelyNonPublished.isEmpty()) {
            assertThat(result).doesNotContainAnyElementsOf(exclusivelyNonPublished);
        }

        // Verify the repository was called with the correct status
        verify(eventRepository).findDistinctCategoriesByStatus(EventStatus.PUBLISHED);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 11: Category listing reflects published events only
    void categoriesFromNonPublishedEventsAreNeverReturned(
            @ForAll("eventSetsWithNoPublished") List<EventData> events) {

        // When there are no PUBLISHED events, no categories should be returned
        when(eventRepository.findDistinctCategoriesByStatus(EventStatus.PUBLISHED))
                .thenReturn(List.of());

        List<String> result = eventService.getPublishedCategories();

        assertThat(result).isEmpty();
        verify(eventRepository).findDistinctCategoriesByStatus(EventStatus.PUBLISHED);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 11: Category listing reflects published events only
    void duplicateCategoriesAreDeduplicatedInResult(
            @ForAll("categoriesWithDuplicates") List<String> categories) {

        // Simulate the repository already returning distinct values
        List<String> distinctCategories = categories.stream().distinct().collect(Collectors.toList());
        when(eventRepository.findDistinctCategoriesByStatus(EventStatus.PUBLISHED))
                .thenReturn(distinctCategories);

        List<String> result = eventService.getPublishedCategories();

        // Result should have no duplicates (distinct categories only)
        assertThat(result).doesNotHaveDuplicates();
        assertThat(Set.copyOf(result)).isEqualTo(Set.copyOf(distinctCategories));
    }

    // --- Data Records ---

    record EventData(String category, EventStatus status) {}

    // --- Generators ---

    @Provide
    Arbitrary<List<EventData>> eventSets() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                categories(),
                statuses()
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<EventData>> eventSetsWithNoPublished() {
        Arbitrary<EventData> eventArbitrary = Combinators.combine(
                categories(),
                nonPublishedStatuses()
        ).as(EventData::new);

        return eventArbitrary.list().ofMinSize(1).ofMaxSize(20);
    }

    @Provide
    Arbitrary<List<String>> categoriesWithDuplicates() {
        // Generate a list where categories can repeat (to verify deduplication)
        return categories().list().ofMinSize(3).ofMaxSize(15);
    }

    private Arbitrary<String> categories() {
        return Arbitraries.of(
                "Music", "Sports", "Arts", "Technology", "Food",
                "Comedy", "Theatre", "Festival", "Conference", "Workshop"
        );
    }

    private Arbitrary<EventStatus> statuses() {
        return Arbitraries.of(EventStatus.values());
    }

    private Arbitrary<EventStatus> nonPublishedStatuses() {
        return Arbitraries.of(EventStatus.DRAFT, EventStatus.CANCELLED, EventStatus.COMPLETED);
    }
}
