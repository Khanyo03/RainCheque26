package com.raincheque.service;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import com.raincheque.entity.TicketBatchJob;
import com.raincheque.entity.User;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketBatchJobRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Property-based test for Batch Job Final Status Determination.
 * Validates: Requirements 14.3
 */
// Feature: security-hardening-cleanup, Property 4: Batch Job Final Status Determination
class BatchJobStatusPropertyTest {

    private BatchTicketService batchTicketService;
    private TicketBatchJobRepository batchJobRepository;
    private BookingRepository bookingRepository;
    private BookingTicketGenerator bookingTicketGenerator;

    @BeforeProperty
    void setUp() {
        batchJobRepository = mock(TicketBatchJobRepository.class);
        bookingRepository = mock(BookingRepository.class);
        bookingTicketGenerator = mock(BookingTicketGenerator.class);

        batchTicketService = new BatchTicketService(
                batchJobRepository,
                bookingRepository,
                bookingTicketGenerator
        );

        // batchJobRepository.save() returns the job as-is
        when(batchJobRepository.save(any(TicketBatchJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 4: Batch Job Final Status Determination
    void jobIsCompletedIfAndOnlyIfAllBookingsSucceed(
            @ForAll("bookingOutcomes") List<Boolean> outcomes) {

        // Create bookings matching the outcomes list
        List<Booking> bookings = createBookings(outcomes.size());

        when(bookingRepository.findConfirmedBookingsByEventId(anyLong()))
                .thenReturn(bookings);

        // Configure mock: for each booking, succeed or throw based on boolean mask
        for (int i = 0; i < bookings.size(); i++) {
            Booking booking = bookings.get(i);
            if (outcomes.get(i)) {
                // Success — return a ticket count
                when(bookingTicketGenerator.generateTicketsForBooking(booking))
                        .thenReturn(2);
            } else {
                // Failure — throw RuntimeException
                when(bookingTicketGenerator.generateTicketsForBooking(booking))
                        .thenThrow(new RuntimeException("Simulated failure for booking " + booking.getId()));
            }
        }

        // Execute and wait for completion
        CompletableFuture<TicketBatchJob> future = batchTicketService.batchGenerateEventTickets(1L, 1L);
        TicketBatchJob job = future.join();

        boolean allSucceeded = outcomes.stream().allMatch(Boolean::booleanValue);

        if (allSucceeded) {
            assertThat(job.getStatus())
                    .as("Job should be COMPLETED when all bookings succeed, outcomes: %s", outcomes)
                    .isEqualTo(TicketBatchJob.JobStatus.COMPLETED);
        } else {
            assertThat(job.getStatus())
                    .as("Job should be FAILED when any booking fails, outcomes: %s", outcomes)
                    .isEqualTo(TicketBatchJob.JobStatus.FAILED);
            assertThat(job.getErrorLog())
                    .as("Error log should be populated when failures occur")
                    .isNotNull()
                    .isNotEmpty();
        }
    }

    // --- Generators ---

    @Provide
    Arbitrary<List<Boolean>> bookingOutcomes() {
        // Generate lists of 1-20 booleans representing success/failure for each booking
        return Arbitraries.of(true, false)
                .list()
                .ofMinSize(1)
                .ofMaxSize(20);
    }

    // --- Helpers ---

    private List<Booking> createBookings(int count) {
        List<Booking> bookings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Booking booking = new Booking();
            booking.setId((long) (i + 1));
            Event event = new Event();
            event.setId(1L);
            booking.setEvent(event);
            bookings.add(booking);
        }
        return bookings;
    }
}
