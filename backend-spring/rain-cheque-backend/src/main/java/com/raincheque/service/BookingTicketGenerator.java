package com.raincheque.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketStatus;
import com.raincheque.entity.TicketTier;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Generates tickets for a booking. Populates the required ticketCode and qrSecret,
 * and re-loads the booking inside the active transaction so lazy associations
 * (ticket tier) resolve correctly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingTicketGenerator {

    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;

    /**
     * Generate missing tickets in a NEW transaction (used by batch generation so one
     * booking's failure does not roll back others).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int generateTicketsForBooking(Booking booking) {
        return doGenerate(booking.getId());
    }

    /**
     * Generate missing tickets in the CURRENT transaction (used by the payment
     * verify/fulfil flow so tickets commit atomically with booking confirmation).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int generateTicketsForBookingInCurrentTx(Booking booking) {
        return doGenerate(booking.getId());
    }

    private int doGenerate(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        List<Ticket> existingTickets = ticketRepository.findByBookingId(booking.getId());
        int missing = booking.getQuantity() - existingTickets.size();

        if (missing <= 0) {
            log.debug("No missing tickets for booking {}", booking.getId());
            return 0;
        }

        TicketTier tier = booking.getTicketTier();

        for (int i = 0; i < missing; i++) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setTicketTier(tier);
            ticket.setPricePaid(tier.getPrice());
            ticket.setStatus(TicketStatus.ACTIVE);
            ticket.setTicketCode(generateTicketCode());
            ticket.setQrSecret(UUID.randomUUID().toString());
            ticketRepository.save(ticket);
        }

        log.debug("Generated {} tickets for booking {}", missing, booking.getId());
        return missing;
    }

    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
