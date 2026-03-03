package com.raincheque.controller;

import com.raincheque.dto.EventResponse;
import com.raincheque.entity.Event;
import com.raincheque.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/events")
@CrossOrigin(origins = "${app.cors.allowed-origins}")
@RequiredArgsConstructor
public class EventController {
    
    private final EventService eventService;
    
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Event> events = eventService.getPublishedEvents(page, size);
        Page<EventResponse> response = events.map(eventService::mapToResponse);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents(
            @RequestParam(defaultValue = "6") int limit) {
        List<Event> events = eventService.getUpcomingEvents(limit);
        List<EventResponse> response = events.stream()
                .map(eventService::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        return ResponseEntity.ok(eventService.mapToResponse(event));
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<EventResponse>> searchEvents(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Event> events = eventService.searchEvents(q, page, size);
        Page<EventResponse> response = events.map(eventService::mapToResponse);
        return ResponseEntity.ok(response);
    }
}