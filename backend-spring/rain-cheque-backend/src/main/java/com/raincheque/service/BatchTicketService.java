package com.raincheque.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.TicketBatchJob;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketBatchJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTicketService {

    private final TicketBatchJobRepository batchJobRepository;
    private final BookingRepository bookingRepository;
    private final BookingTicketGenerator bookingTicketGenerator;

    /**
     * Generate tickets for all confirmed bookings of an event.
     * The job is saved synchronously, then ticket generation runs asynchronously
     * with each booking processed in its own transaction via BookingTicketGenerator.
     */
    public CompletableFuture<TicketBatchJob> batchGenerateEventTickets(Long eventId, Long userId) {
        log.info("Starting batch ticket generation for event {} by user {}", eventId, userId);

        TicketBatchJob job = new TicketBatchJob();
        job.setEvent(new com.raincheque.entity.Event(eventId));
        job.setCreatedBy(new com.raincheque.entity.User(userId));
        job.setStatus(TicketBatchJob.JobStatus.IN_PROGRESS);
        job.setCreatedAt(LocalDateTime.now());
        job.setTotalTickets(0);
        job.setJobName("Batch ticket generation for event " + eventId);

        batchJobRepository.save(job);

        return CompletableFuture.runAsync(() -> {
            int totalTickets = 0;
            boolean hasFailures = false;
            StringBuilder errorLog = new StringBuilder();

            List<Booking> confirmedBookings = bookingRepository.findConfirmedBookingsByEventId(eventId);
            if (confirmedBookings.isEmpty()) {
                log.warn("No confirmed bookings found for event {}", eventId);
                job.setStatus(TicketBatchJob.JobStatus.FAILED);
                job.setErrorLog("No confirmed bookings found for event " + eventId);
                job.setCompletedAt(LocalDateTime.now());
                batchJobRepository.save(job);
                return;
            }

            for (Booking booking : confirmedBookings) {
                try {
                    int generated = bookingTicketGenerator.generateTicketsForBooking(booking);
                    totalTickets += generated;
                } catch (Exception e) {
                    hasFailures = true;
                    String errorMsg = String.format("Failed to generate tickets for booking %d: %s",
                            booking.getId(), e.getMessage());
                    log.error(errorMsg, e);
                    if (errorLog.length() > 0) {
                        errorLog.append("\n");
                    }
                    errorLog.append(errorMsg);
                }
            }

            job.setProcessedTickets(totalTickets);
            job.setTotalTickets(totalTickets);
            job.setCompletedAt(LocalDateTime.now());

            if (hasFailures) {
                job.setStatus(TicketBatchJob.JobStatus.FAILED);
                job.setErrorLog(errorLog.toString());
            } else {
                job.setStatus(TicketBatchJob.JobStatus.COMPLETED);
            }

            batchJobRepository.save(job);
            log.info("Batch ticket generation {} for event {}. Total tickets: {}",
                    hasFailures ? "completed with errors" : "completed successfully",
                    eventId, totalTickets);
        }).thenApply(v -> job);
    }
}
