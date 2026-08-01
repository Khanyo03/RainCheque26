package com.raincheque.service;

import com.raincheque.entity.*;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketBatchJobRepository;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based test for Batch Transaction Isolation.
 * Validates: Requirements 14.1, 14.2, 14.4
 *
 * Verifies that when processing a batch of bookings, failures for some bookings
 * do not affect successful bookings, and errors are properly logged.
 */
// Feature: security-hardening-cleanup, Property 3: Batch Transaction Isolation
class BatchTransactionIsolationPropertyTest {

    private BookingRepository bookingRepository;
    private TicketBatchJobRepository batchJobRepository;
    private BookingTicketGenerator bookingTicketGenerator;
    private BatchTicketService batchTicketService;

    @BeforeProperty
    void setUp() {
        bookingRepository = Mockito.mock(BookingRepository.class);
        batchJobRepository = Mockito.mock(TicketBatchJobRepository.class);
        bookingTicketGenerator = Mockito.mock(BookingTicketGenerator.class);
        batchTicketService = new BatchTicketService(batchJobRepository, bookingRepository, bookingTicketGenerator);

        // Make save return the argument for TicketBatchJob
        when(batchJobRepository.save(any(TicketBatchJob.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 3: Batch Transaction Isolation
    void successfulBookingsPreservedAndFailedBookingsLoggedInErrorLog(
            @ForAll("bookingSetsWithFailures") BookingScenario scenario) throws Exception {

        Long eventId = 1L;
        Long userId = 1L;

        // Set up the repository to return all bookings
        when(bookingRepository.findConfirmedBookingsByEventId(eventId))
                .thenReturn(scenario.allBookings);

        // Configure the generator: succeed for some, fail for others
        for (Booking booking : scenario.successfulBookings) {
            when(bookingTicketGenerator.generateTicketsForBooking(booking))
                    .thenReturn(booking.getQuantity());
        }
        for (Booking booking : scenario.failingBookings) {
            when(bookingTicketGenerator.generateTicketsForBooking(booking))
                    .thenThrow(new RuntimeException("Simulated failure for booking " + booking.getId()));
        }

        // Execute the batch and wait for completion
        CompletableFuture<TicketBatchJob> future = batchTicketService.batchGenerateEventTickets(eventId, userId);
        TicketBatchJob resultJob = future.get();

        // PROPERTY: For successful bookings, the generator was called (tickets persisted in its own transaction)
        for (Booking booking : scenario.successfulBookings) {
            verify(bookingTicketGenerator).generateTicketsForBooking(booking);
        }

        // PROPERTY: For failed bookings, the generator was also called (attempted but failed)
        for (Booking booking : scenario.failingBookings) {
            verify(bookingTicketGenerator).generateTicketsForBooking(booking);
        }

        // PROPERTY: Successful bookings' ticket count is preserved in the job's processed total
        int expectedTotalTickets = scenario.successfulBookings.stream()
                .mapToInt(Booking::getQuantity)
                .sum();
        assertThat(resultJob.getProcessedTickets()).isEqualTo(expectedTotalTickets);

        // PROPERTY: Failed bookings have their errors logged in errorLog
        if (!scenario.failingBookings.isEmpty()) {
            assertThat(resultJob.getErrorLog()).isNotNull();
            for (Booking failedBooking : scenario.failingBookings) {
                assertThat(resultJob.getErrorLog())
                        .contains("booking " + failedBooking.getId());
            }
            // Job status should be FAILED when there are failures
            assertThat(resultJob.getStatus()).isEqualTo(TicketBatchJob.JobStatus.FAILED);
        } else {
            // If no failures, job should be COMPLETED and errorLog should be null/empty
            assertThat(resultJob.getStatus()).isEqualTo(TicketBatchJob.JobStatus.COMPLETED);
        }

        // PROPERTY: The batch job was saved (persisted) with final state
        // At least 2 saves: initial IN_PROGRESS + final state
        verify(batchJobRepository, atLeast(2)).save(any(TicketBatchJob.class));
    }

    // --- Scenario and Generators ---

    /**
     * Represents a test scenario with a mix of successful and failing bookings.
     */
    static class BookingScenario {
        final List<Booking> allBookings;
        final List<Booking> successfulBookings;
        final List<Booking> failingBookings;

        BookingScenario(List<Booking> successfulBookings, List<Booking> failingBookings) {
            this.successfulBookings = successfulBookings;
            this.failingBookings = failingBookings;
            this.allBookings = new ArrayList<>();
            this.allBookings.addAll(successfulBookings);
            this.allBookings.addAll(failingBookings);
            // Shuffle to ensure order doesn't matter
            Collections.shuffle(this.allBookings);
        }

        @Override
        public String toString() {
            return String.format("BookingScenario{total=%d, success=%d, failures=%d}",
                    allBookings.size(), successfulBookings.size(), failingBookings.size());
        }
    }

    @Provide
    Arbitrary<BookingScenario> bookingSetsWithFailures() {
        // Generate 1-10 total bookings, with at least 1 failure injected
        return Arbitraries.integers().between(2, 10).flatMap(totalCount ->
            Arbitraries.integers().between(1, Math.max(1, totalCount - 1)).flatMap(failureCount -> {
                int successCount = totalCount - failureCount;
                return Combinators.combine(
                    bookingList(successCount, 1L),
                    bookingList(failureCount, (long) successCount + 1L)
                ).as(BookingScenario::new);
            })
        );
    }

    private Arbitrary<List<Booking>> bookingList(int count, long startId) {
        if (count <= 0) {
            return Arbitraries.just(Collections.emptyList());
        }
        return Arbitraries.integers().between(1, 5).list().ofSize(count)
                .map(quantities -> {
                    List<Booking> bookings = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        Booking booking = createBooking(startId + i, quantities.get(i));
                        bookings.add(booking);
                    }
                    return bookings;
                });
    }

    private Booking createBooking(long id, int quantity) {
        Event event = new Event();
        event.setId(1L);

        TicketTier tier = new TicketTier();
        tier.setId(1L);
        tier.setName("General");
        tier.setPrice(BigDecimal.valueOf(50.00));

        Booking booking = new Booking();
        setFieldValue(booking, "id", id);
        booking.setEvent(event);
        booking.setTicketTier(tier);
        booking.setQuantity(quantity);
        booking.setTotalAmount(BigDecimal.valueOf(quantity * 50.00));
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setBookingTime(LocalDateTime.now());

        return booking;
    }

    /**
     * Helper to set private fields via reflection.
     */
    private static void setFieldValue(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
