package com.raincheque.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketBatchJob;
import com.raincheque.entity.TicketStatus;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.EventRepository;
import com.raincheque.repository.TicketBatchJobRepository;
import com.raincheque.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTicketService {

    private final TicketRepository ticketRepository;
    private final TicketBatchJobRepository batchJobRepository;
    private final BookingRepository bookingRepository;

    /**
     * Generate tickets for all confirmed bookings of an event.
     */
    @Transactional
    public CompletableFuture<TicketBatchJob> batchGenerateEventTickets(Long eventId, Long userId) {
        log.info("Starting batch ticket generation for event {} by user {}", eventId, userId);

        TicketBatchJob job = new TicketBatchJob();
        job.setEvent(new com.raincheque.entity.Event(eventId));
        job.setCreatedBy(new com.raincheque.entity.User(userId)); 

        job.setStatus(TicketBatchJob.JobStatus.IN_PROGRESS);
        job.setCreatedAt(LocalDateTime.now());

        batchJobRepository.save(job);

        return CompletableFuture.supplyAsync(() -> {
            int totalTickets = 0;
            int processed = 0;

            List<Booking> confirmedBookings = bookingRepository.findConfirmedBookingsByEventId(eventId);
            if (confirmedBookings.isEmpty()) {
                log.warn("No confirmed bookings found for event {}", eventId);
                throw new ResourceNotFoundException("No confirmed bookings found for event");
            }

            for (Booking booking : confirmedBookings) {
                List<Ticket> existingTickets = ticketRepository.findByBookingId(booking.getId());
                int missing = booking.getQuantity() - existingTickets.size();

                for (int i = 0; i < missing; i++) {
                    Ticket ticket = new Ticket();
                    ticket.setBooking(booking);
                    ticket.setTicketTier(booking.getTicketTier());
                    ticket.setPricePaid(booking.getTicketTier().getPrice());
                    ticket.setStatus(TicketStatus.ACTIVE); 
                    ticketRepository.save(ticket);
                    totalTickets++;
                }

                processed++;

                if (processed % 10 == 0) {
                    job.setProcessedTickets(totalTickets);
                    batchJobRepository.save(job);
                    log.debug("Progress update: {} tickets processed", totalTickets);
                }
            }

            job.setProcessedTickets(totalTickets);
            job.setTotalTickets(totalTickets);
            job.setStatus(TicketBatchJob.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            batchJobRepository.save(job);

            log.info("Batch ticket generation completed for event {}. Total tickets: {}", eventId, totalTickets);
            return job;
        });
    }
}
