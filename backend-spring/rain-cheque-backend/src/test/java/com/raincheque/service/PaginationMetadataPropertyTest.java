package com.raincheque.service;

import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.entity.Venue;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.VenueRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeTry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for pagination metadata consistency.
 * Validates: Requirements 8.2
 *
 * For any paginated response, the pagination metadata SHALL satisfy:
 * totalPages == ceil(totalElements / pageSize)
 * content.length <= pageSize
 * currentPage == requested page number
 */
// Feature: event-management, Property 15: Pagination metadata consistency
class PaginationMetadataPropertyTest {

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
    // Feature: event-management, Property 15: Pagination metadata consistency
    void paginationMetadataIsConsistentForAnyPageParameters(
            @ForAll("totalElements") int totalElements,
            @ForAll("pageSizes") int pageSize) {

        // Calculate expected total pages
        int expectedTotalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Choose a valid page number (0-indexed)
        int pageNumber = expectedTotalPages > 0 ? 0 : 0;
        // For first page: content size = min(pageSize, totalElements)
        int contentSize = Math.min(pageSize, totalElements);

        // Build mock events for the page content
        List<Event> pageContent = createMockEvents(contentSize);

        // Create PageImpl with the known totals
        Page<Event> mockPage = new PageImpl<>(pageContent, PageRequest.of(pageNumber, pageSize), totalElements);

        // Mock repository to return our page
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Call the service
        Page<EventResponse> result = eventService.getAllEventsAdmin(
                PageRequest.of(pageNumber, pageSize), null, null, null);

        // Assert: totalPages == ceil(totalElements / pageSize)
        assertThat(result.getTotalPages()).isEqualTo(expectedTotalPages);

        // Assert: content.size() <= pageSize
        assertThat(result.getContent().size()).isLessThanOrEqualTo(pageSize);

        // Assert: currentPage == requested page number
        assertThat(result.getNumber()).isEqualTo(pageNumber);

        // Assert: totalElements matches
        assertThat(result.getTotalElements()).isEqualTo(totalElements);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 15: Pagination metadata consistency
    void paginationMetadataIsConsistentForArbitraryPageNumber(
            @ForAll("totalElements") int totalElements,
            @ForAll("pageSizes") int pageSize,
            @ForAll("pageNumbers") int rawPageNumber) {

        // Calculate expected total pages
        int expectedTotalPages = (int) Math.ceil((double) totalElements / pageSize);

        // Constrain page number to valid range (0 to totalPages-1), or 0 if totalPages is 0
        int pageNumber;
        if (expectedTotalPages == 0) {
            pageNumber = 0;
        } else {
            pageNumber = Math.abs(rawPageNumber) % expectedTotalPages;
        }

        // Calculate content size for this specific page
        int contentSize;
        if (totalElements == 0) {
            contentSize = 0;
        } else {
            int remainingElements = totalElements - (pageNumber * pageSize);
            contentSize = Math.min(pageSize, remainingElements);
        }

        // Build mock events for the page content
        List<Event> pageContent = createMockEvents(contentSize);

        // Create PageImpl with the known totals
        Page<Event> mockPage = new PageImpl<>(pageContent, PageRequest.of(pageNumber, pageSize), totalElements);

        // Mock repository to return our page
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        // Call the service
        Page<EventResponse> result = eventService.getAllEventsAdmin(
                PageRequest.of(pageNumber, pageSize), null, null, null);

        // Assert: totalPages == ceil(totalElements / pageSize)
        assertThat(result.getTotalPages()).isEqualTo(expectedTotalPages);

        // Assert: content.size() <= pageSize
        assertThat(result.getContent().size()).isLessThanOrEqualTo(pageSize);

        // Assert: currentPage == requested page number
        assertThat(result.getNumber()).isEqualTo(pageNumber);
    }

    @Property(tries = 100)
    // Feature: event-management, Property 15: Pagination metadata consistency
    void emptyResultSetHasConsistentMetadata(
            @ForAll("pageSizes") int pageSize,
            @ForAll("pageNumbers") int rawPageNumber) {

        int pageNumber = Math.abs(rawPageNumber) % 10; // Any page number for empty results

        // Empty page
        Page<Event> mockPage = new PageImpl<>(List.of(), PageRequest.of(pageNumber, pageSize), 0);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<EventResponse> result = eventService.getAllEventsAdmin(
                PageRequest.of(pageNumber, pageSize), null, null, null);

        // For empty result: totalPages == 0, content is empty, total elements is 0
        assertThat(result.getTotalPages()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getNumber()).isEqualTo(pageNumber);
    }

    // --- Helper Methods ---

    private List<Event> createMockEvents(int count) {
        List<Event> events = new ArrayList<>();
        Venue mockVenue = new Venue();
        mockVenue.setId(1L);
        mockVenue.setName("Test Venue");
        mockVenue.setCity("Test City");
        mockVenue.setAddress("123 Test St");

        for (int i = 0; i < count; i++) {
            Event event = new Event();
            event.setId((long) (i + 1));
            event.setTitle("Event " + (i + 1));
            event.setDescription("Description " + (i + 1));
            event.setShortDescription("Short desc " + (i + 1));
            event.setVenue(mockVenue);
            event.setCategory("Music");
            event.setStatus(Event.EventStatus.PUBLISHED);
            event.setStartTime(LocalDateTime.now().plusDays(i + 1));
            event.setEndTime(LocalDateTime.now().plusDays(i + 1).plusHours(2));
            event.setFeatured(false);
            event.setTicketTiers(List.of());
            events.add(event);
        }
        return events;
    }

    // --- Generators ---

    @Provide
    Arbitrary<Integer> totalElements() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> pageSizes() {
        return Arbitraries.integers().between(1, 50);
    }

    @Provide
    Arbitrary<Integer> pageNumbers() {
        return Arbitraries.integers().between(0, 99);
    }
}
