package com.raincheque.dto;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.Ticket;
import com.raincheque.entity.Venue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketCode,
        String status,
        String seatNumber,
        BigDecimal pricePaid,
        LocalDateTime usedAt,
        String eventName,
        String tierName,
        Long bookingId,
        LocalDateTime eventStartTime,
        String venueName
) {
    public static TicketResponse from(Ticket ticket) {
        Booking booking = ticket.getBooking();
        Event event = booking != null ? booking.getEvent() : null;
        Venue venue = event != null ? event.getVenue() : null;

        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getStatus() != null ? ticket.getStatus().name() : null,
                ticket.getSeatNumber(),
                ticket.getPricePaid(),
                ticket.getUsedAt(),
                event != null ? event.getTitle() : null,
                ticket.getTicketTier() != null ? ticket.getTicketTier().getName() : null,
                booking != null ? booking.getId() : null,
                event != null ? event.getStartTime() : null,
                venue != null ? venue.getName() : null
        );
    }
}
