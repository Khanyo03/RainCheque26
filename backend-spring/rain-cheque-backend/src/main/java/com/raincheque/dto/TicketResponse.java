package com.raincheque.dto;

import com.raincheque.entity.Ticket;

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
        Long bookingId
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getStatus() != null ? ticket.getStatus().name() : null,
                ticket.getSeatNumber(),
                ticket.getPricePaid(),
                ticket.getUsedAt(),
                ticket.getBooking() != null && ticket.getBooking().getEvent() != null
                        ? ticket.getBooking().getEvent().getTitle()
                        : null,
                ticket.getTicketTier() != null
                        ? ticket.getTicketTier().getName()
                        : null,
                ticket.getBooking() != null
                        ? ticket.getBooking().getId()
                        : null
        );
    }
}
