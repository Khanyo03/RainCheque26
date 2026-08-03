package com.raincheque.service;

import com.raincheque.dto.EventRequest;
import com.raincheque.dto.EventResponse;
import com.raincheque.dto.StatusRequest;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketTier;
import com.raincheque.entity.Venue;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.EventSpecifications;
import com.raincheque.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    
    /**
     * Get all published events with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getPublishedEvents(int page, int size) {
        log.info("Fetching published events - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        return eventRepository.findByStatus(Event.EventStatus.PUBLISHED, pageable);
    }
    
    /**
     * Get featured events
     */
    @Transactional(readOnly = true)
    public List<Event> getFeaturedEvents(int limit) {
        log.info("Fetching featured events - limit: {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return eventRepository.findByStatusAndFeatured(Event.EventStatus.PUBLISHED, true, pageable);
    }
    
    /**
     * Get upcoming events
     */
    @Transactional(readOnly = true)
    public List<Event> getUpcomingEvents(int limit) {
        log.info("Fetching upcoming events - limit: {}", limit);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("startTime").ascending());
        return eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
    }
    
    /**
     * Get event by ID
     */
    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        log.info("Fetching event by ID: {}", id);
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
    }
    
    /**
     * Search events by keyword
     */
    @Transactional(readOnly = true)
    public Page<Event> searchEvents(String keyword, int page, int size) {
        log.info("Searching events with keyword: {} - page: {}, size: {}", keyword, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.searchEvents(keyword, pageable);
    }
    
    /**
     * Get events by category
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsByCategory(String category, int page, int size) {
        log.info("Fetching events by category: {} - page: {}, size: {}", category, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").ascending());
        return eventRepository.findByCategoryAndStatus(category, Event.EventStatus.PUBLISHED, pageable);
    }
    
    /**
     * Get events by venue
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByVenue(Long venueId, LocalDateTime start, LocalDateTime end) {
        log.info("Fetching events for venue: {} between {} and {}", venueId, start, end);
        return eventRepository.findByVenueIdAndStartTimeBetween(venueId, start, end);
    }
    
    /**
     * Create new event
     */
    @Transactional
    public Event createEvent(Event event) {
        log.info("Creating new event: {}", event.getTitle());
        event.setStatus(Event.EventStatus.DRAFT);
        return eventRepository.save(event);
    }
    
    /**
     * Update event
     */
    @Transactional
    public Event updateEvent(Long id, Event eventDetails) {
        log.info("Updating event with ID: {}", id);
        Event event = getEventById(id);
        
        event.setTitle(eventDetails.getTitle());
        event.setDescription(eventDetails.getDescription());
        event.setShortDescription(eventDetails.getShortDescription());
        event.setVenue(eventDetails.getVenue());
        event.setStartTime(eventDetails.getStartTime());
        event.setEndTime(eventDetails.getEndTime());
        event.setCategory(eventDetails.getCategory());
        event.setBannerImageUrl(eventDetails.getBannerImageUrl());
        event.setThumbnailUrl(eventDetails.getThumbnailUrl());
        event.setMetadata(eventDetails.getMetadata());
        event.setFeatured(eventDetails.isFeatured());
        
        return eventRepository.save(event);
    }
    
    /**
     * Update event status
     */
    @Transactional
    public Event updateEventStatus(Long id, Event.EventStatus status) {
        log.info("Updating event status - ID: {}, new status: {}", id, status);
        Event event = getEventById(id);
        event.setStatus(status);
        return eventRepository.save(event);
    }
    
    /**
     * Delete event
     */
    @Transactional
    public void deleteEvent(Long id) {
        log.info("Deleting event with ID: {}", id);
        Event event = getEventById(id);
        eventRepository.delete(event);
    }
    
    // =====================================================
    // Admin CRUD and search methods (Task 2.5)
    // =====================================================

    /**
     * Get all events for admin listing with optional filters.
     * Returns all statuses, sorted by creation date descending.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEventsAdmin(Pageable pageable, Event.EventStatus status, String category, String search) {
        log.info("Admin listing events - status: {}, category: {}, search: {}", status, category, search);
        Specification<Event> spec = EventSpecifications.withFilters(status, category, search);
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id")
        );
        Page<Event> events = eventRepository.findAll(spec, sortedPageable);
        return events.map(this::mapToResponse);
    }

    /**
     * Search events with conjunctive filters: keyword, category, date range.
     * All non-null filters are applied together (AND).
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> searchEventsWithFilters(String keyword, String category, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        log.info("Searching events with filters - keyword: {}, category: {}, from: {}, to: {}", keyword, category, from, to);

        Specification<Event> spec = (root, query, cb) -> cb.conjunction();

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(EventSpecifications.containsKeyword(keyword));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and(EventSpecifications.hasCategory(category));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), to));
        }

        Page<Event> events = eventRepository.findAll(spec, pageable);
        return events.map(this::mapToResponse);
    }

    /**
     * Create a new event from a request DTO.
     * Always assigns DRAFT status regardless of any other input.
     */
    @Transactional
    public EventResponse createEvent(EventRequest request) {
        log.info("Creating new event from request: {}", request.title());

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));

        Event event = new Event();
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setShortDescription(request.shortDescription());
        event.setVenue(venue);
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setCategory(request.category());
        event.setFeatured(request.featured() != null ? request.featured() : false);
        event.setMetadata(request.metadata());
        event.setStatus(Event.EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Update an existing event from a request DTO.
     * Throws ResourceNotFoundException if the event does not exist.
     */
    @Transactional
    public EventResponse updateEvent(Long id, EventRequest request) {
        log.info("Updating event from request - ID: {}", id);

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        Venue venue = venueRepository.findById(request.venueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.venueId()));

        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setShortDescription(request.shortDescription());
        event.setVenue(venue);
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setCategory(request.category());
        event.setFeatured(request.featured() != null ? request.featured() : false);
        event.setMetadata(request.metadata());

        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Update only the status of an existing event.
     * Throws ResourceNotFoundException if the event does not exist.
     */
    @Transactional
    public EventResponse updateStatus(Long id, StatusRequest request) {
        log.info("Updating event status from request - ID: {}, new status: {}", id, request.status());

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

        event.setStatus(request.status());
        Event saved = eventRepository.save(event);
        return mapToResponse(saved);
    }

    /**
     * Get all categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        log.info("Fetching all event categories");
        return eventRepository.findAllCategories();
    }

    /**
     * Get distinct categories from published events only.
     */
    @Transactional(readOnly = true)
    public List<String> getPublishedCategories() {
        log.info("Fetching distinct categories from published events");
        return eventRepository.findDistinctCategoriesByStatus(Event.EventStatus.PUBLISHED);
    }
    
    /**
     * Get all cities where events are held
     */
    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        log.info("Fetching all event cities");
        return eventRepository.findAllCities();
    }
    
    /**
     * Check if event has available tickets
     */
    @Transactional(readOnly = true)
    public boolean hasAvailableTickets(Long eventId) {
        Event event = getEventById(eventId);
        return event.getTicketTiers().stream()
                .anyMatch(tier -> tier.getRemainingQuantity() > 0);
    }
    
    /**
     * Get total available tickets count
     */
    @Transactional(readOnly = true)
    public int getTotalAvailableTickets(Long eventId) {
        Event event = getEventById(eventId);
        return event.getTicketTiers().stream()
                .mapToInt(TicketTier::getRemainingQuantity)
                .sum();
    }
    
    /**
     * Map Event entity to EventResponse DTO
     */
    public EventResponse mapToResponse(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setTitle(event.getTitle());
        response.setDescription(event.getDescription());
        response.setShortDescription(event.getShortDescription());
        
        if (event.getVenue() != null) {
            response.setVenueName(event.getVenue().getName());
            response.setVenueCity(event.getVenue().getCity());
            response.setVenueAddress(event.getVenue().getAddress());
        }
        
        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());
        response.setCategory(event.getCategory());
        response.setBannerImageUrl(event.getBannerImageUrl());
        response.setThumbnailUrl(event.getThumbnailUrl());
        response.setStatus(event.getStatus());
        response.setFeatured(event.isFeatured());
        
        if (event.getMetadata() != null) {
            response.setMetadata(event.getMetadata());
        }
        
        if (event.getTicketTiers() != null && !event.getTicketTiers().isEmpty()) {
            List<EventResponse.TicketTierDto> tierDtos = event.getTicketTiers().stream()
                    .filter(TicketTier::getIsActive)
                    .map(this::mapTierToDto)
                    .collect(Collectors.toList());
            response.setTicketTiers(tierDtos);
            
            // Calculate price range
            BigDecimal minPrice = tierDtos.stream()
                    .map(EventResponse.TicketTierDto::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal maxPrice = tierDtos.stream()
                    .map(EventResponse.TicketTierDto::getPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            response.setMinPrice(minPrice);
            response.setMaxPrice(maxPrice);
        }
        
        return response;
    }
    
    /**
     * Map TicketTier entity to TicketTierDto
     */
    private EventResponse.TicketTierDto mapTierToDto(TicketTier tier) {
        EventResponse.TicketTierDto dto = new EventResponse.TicketTierDto();
        dto.setId(tier.getId());
        dto.setName(tier.getName());
        dto.setDescription(tier.getDescription());
        dto.setPrice(tier.getPrice());
        dto.setRemainingQuantity(tier.getRemainingQuantity());
        dto.setTotalQuantity(tier.getTotalQuantity());
        dto.setMaxPerUser(tier.getMaxPerUser());
        dto.setMinPerUser(tier.getMinPerUser());
        dto.setBenefits(tier.getBenefits());
        dto.setActive(tier.getIsActive());
        return dto;
    }
    
    /**
     * Map list of Events to list of EventResponse DTOs
     */
    public List<EventResponse> mapToResponseList(List<Event> events) {
        return events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Get events by date range
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        log.info("Fetching events between {} and {}", start, end);
        return eventRepository.findByStartTimeBetween(start, end);
    }
    
    /**
     * Get popular events (based on ticket sales)
     */
    @Transactional(readOnly = true)
    public List<Event> getPopularEvents(int limit) {
        log.info("Fetching popular events - limit: {}", limit);
        Pageable pageable = PageRequest.of(0, limit);
        return eventRepository.findPopularEvents(pageable);
    }
    
    /**
     * Get recommended events for user (based on user's past bookings)
     */
    @Transactional(readOnly = true)
    public List<Event> getRecommendedEvents(Long userId, int limit) {
        log.info("Fetching recommended events for user: {} - limit: {}", userId, limit);
        Pageable pageable = PageRequest.of(0, limit);
        return eventRepository.findRecommendedEvents(userId, pageable);
    }
}