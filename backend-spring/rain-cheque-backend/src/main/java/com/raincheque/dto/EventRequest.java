package com.raincheque.dto;

import com.raincheque.entity.Event;
import com.raincheque.validation.ValidTimeRange;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request DTO for creating and updating events.
 */
@ValidTimeRange
public record EventRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
        String title,

        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,

        @Size(max = 500, message = "Short description must not exceed 500 characters")
        String shortDescription,

        @NotNull(message = "Venue is required")
        Long venueId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        LocalDateTime endTime,

        @NotBlank(message = "Category is required")
        String category,

        Boolean featured,

        Event.EventMetadata metadata
) {
}
