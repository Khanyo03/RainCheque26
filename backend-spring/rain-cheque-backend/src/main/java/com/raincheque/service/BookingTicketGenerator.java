package com.raincheque.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketStatus;
import com.raincheque.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Generates tickets for a single booking within its own transactional boundary.
 * Each invocation runs in a REQUIRES_NEW transaction so that failures
 * for one booking do not roll back tickets generated for other bookings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingTicketGenerator {

    private final TicketRepository ticketRepository;

    /**
     * Generate missing tickets for the given booking.
     * Runs in a new transaction that commits independently.
     *
     * @param booking the confirmed booking to generate tickets for
     * @return the number of tickets generated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int generateTicketsForBooking(Booking booking) {
        List<Ticket> existingTickets = ticketRepository.findByBookingId(booking.getId());
        int missing = booking.getQuantity() - existingTickets.size();

        if (missing <= 0) {
            log.debug("No missing tickets for booking {}", booking.getId());
            return 0;
        }

        for (int i = 0; i < missing; i++) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setTicketTier(booking.getTicketTier());
            ticket.setPricePaid(booking.getTicketTier().getPrice());
            ticket.setStatus(TicketStatus.ACTIVE);
            ticketRepository.save(ticket);
        }

        log.debug("Generated {} tickets for booking {}", missing, booking.getId());
        return missing;
    }
}
