package com.raincheque.dto;

import com.raincheque.entity.Event;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating event status.
 */
public record StatusRequest(
        @NotNull(message = "Status is required")
        Event.EventStatus status
) {
}
