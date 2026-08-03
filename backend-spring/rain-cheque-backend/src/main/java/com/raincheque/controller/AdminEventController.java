package com.raincheque.controller;

import com.raincheque.dto.EventRequest;
import com.raincheque.dto.EventResponse;
import com.raincheque.dto.ImageUploadResponse;
import com.raincheque.dto.StatusRequest;
import com.raincheque.entity.Event;
import com.raincheque.service.EventService;
import com.raincheque.service.ImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventService eventService;
    private final ImageUploadService imageUploadService;

    /**
     * Create a new event. Always persisted with DRAFT status.
     */
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all events with optional filters (status, category, search) and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> listEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Event.EventStatus status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponse> events = eventService.getAllEventsAdmin(pageable, status, category, search);
        return ResponseEntity.ok(events);
    }

    /**
     * Get a single event by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable Long id) {
        Event event = eventService.getEventById(id);
        EventResponse response = eventService.mapToResponse(event);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing event.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.updateEvent(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update the status of an existing event.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        EventResponse response = eventService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an event. Returns 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload an image for an event. Updates the event's bannerImageUrl or thumbnailUrl
     * based on the target parameter.
     */
    @PostMapping("/{id}/images")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("target") String target) {
        // Upload the image file
        String imageUrl = imageUploadService.uploadImage(file);

        // Update the event's image URL based on target
        Event event = eventService.getEventById(id);
        if ("banner".equalsIgnoreCase(target)) {
            event.setBannerImageUrl(imageUrl);
        } else if ("thumbnail".equalsIgnoreCase(target)) {
            event.setThumbnailUrl(imageUrl);
        }
        eventService.updateEvent(id, event);

        // Extract filename from the URL
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);

        return ResponseEntity.ok(new ImageUploadResponse(imageUrl, filename, target));
    }
}
