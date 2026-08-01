package com.raincheque.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.raincheque.entity.*;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for TicketResponse DTO mapping.
 * Validates: Requirements 13.1, 13.2, 13.3
 */
// Feature: security-hardening-cleanup, Property 2: Ticket DTO Mapping Completeness and Safety
class TicketResponsePropertyTest {

    private ObjectMapper objectMapper;

    @BeforeProperty
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Property(tries = 100)
    // Feature: security-hardening-cleanup, Property 2: Ticket DTO Mapping Completeness and Safety
    void mappedDtoFieldsMatchSourceEntityAndExcludeSensitiveData(
            @ForAll("fullyPopulatedTickets") Ticket ticket) throws JsonProcessingException {

        // Map entity to DTO
        TicketResponse dto = TicketResponse.from(ticket);

        // Assert all fields are non-null and match their source
        assertThat(dto.id()).isNotNull().isEqualTo(ticket.getId());
        assertThat(dto.ticketCode()).isNotNull().isEqualTo(ticket.getTicketCode());
        assertThat(dto.status()).isNotNull().isEqualTo(ticket.getStatus().name());
        assertThat(dto.seatNumber()).isNotNull().isEqualTo(ticket.getSeatNumber());
        assertThat(dto.pricePaid()).isNotNull().isEqualTo(ticket.getPricePaid());
        assertThat(dto.usedAt()).isNotNull().isEqualTo(ticket.getUsedAt());
        assertThat(dto.eventName()).isNotNull().isEqualTo(ticket.getBooking().getEvent().getTitle());
        assertThat(dto.tierName()).isNotNull().isEqualTo(ticket.getTicketTier().getName());
        assertThat(dto.bookingId()).isNotNull().isEqualTo(ticket.getBooking().getId());

        // Serialize DTO to JSON and verify no sensitive data leaks
        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).doesNotContain("qrSecret");
        assertThat(json).doesNotContain("qr_secret");
        // Check for JPA proxy class name patterns (Hibernate proxies)
        assertThat(json).doesNotContain("HibernateProxy");
        assertThat(json).doesNotContain("$HibernateProxy");
        assertThat(json).doesNotContain("ByteBuddyInterceptor");
        assertThat(json).doesNotContain("$$_javassist");
        assertThat(json).doesNotContain("_$$_jvst");
    }

    // --- Providers / Generators ---

    @Provide
    Arbitrary<Ticket> fullyPopulatedTickets() {
        // Use flatMap chaining since jqwik Combinators.combine supports max 8 args
        return Combinators.combine(
            ticketIds(),
            ticketCodes(),
            ticketStatuses(),
            seatNumbers(),
            prices(),
            usedAtTimes(),
            eventNames(),
            tierNames()
        ).flatAs((id, code, status, seat, price, usedAt, eventName, tierName) ->
            Combinators.combine(bookingIds(), qrSecrets()).as((bookingId, qrSecret) -> {
                // Build Event with title
                Event event = new Event();
                event.setId(bookingId + 1000L);
                event.setTitle(eventName);

                // Build Booking with event and id
                Booking booking = new Booking();
                booking.setId(bookingId);
                booking.setEvent(event);

                // Build TicketTier with name
                TicketTier ticketTier = new TicketTier();
                ticketTier.setId(id + 500L);
                ticketTier.setName(tierName);

                // Build Ticket entity with all associations populated
                Ticket ticket = new Ticket();
                ticket.setTicketCode(code);
                ticket.setStatus(status);
                ticket.setSeatNumber(seat);
                ticket.setPricePaid(price);
                ticket.setUsedAt(usedAt);
                ticket.setBooking(booking);
                ticket.setTicketTier(ticketTier);

                // Use reflection to set id and qrSecret (no public setters)
                setFieldValue(ticket, "id", id);
                setFieldValue(ticket, "qrSecret", qrSecret);

                return ticket;
            })
        );
    }

    private Arbitrary<Long> ticketIds() {
        return Arbitraries.longs().between(1L, 100000L);
    }

    private Arbitrary<String> ticketCodes() {
        return Arbitraries.strings().alpha().numeric().ofMinLength(8).ofMaxLength(20)
                .map(s -> "TKT-" + s);
    }

    private Arbitrary<TicketStatus> ticketStatuses() {
        return Arbitraries.of(TicketStatus.values());
    }

    private Arbitrary<String> seatNumbers() {
        return Combinators.combine(
            Arbitraries.chars().range('A', 'Z'),
            Arbitraries.integers().between(1, 99)
        ).as((row, num) -> String.valueOf(row) + num);
    }

    private Arbitrary<BigDecimal> prices() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ONE, new BigDecimal("9999.99"))
                .ofScale(2);
    }

    private Arbitrary<LocalDateTime> usedAtTimes() {
        return Arbitraries.integers().between(2020, 2025).flatMap(year ->
            Arbitraries.integers().between(1, 12).flatMap(month ->
                Arbitraries.integers().between(1, 28).flatMap(day ->
                    Arbitraries.integers().between(0, 23).flatMap(hour ->
                        Arbitraries.integers().between(0, 59).map(minute ->
                            LocalDateTime.of(year, month, day, hour, minute)
                        )
                    )
                )
            )
        );
    }

    private Arbitrary<String> eventNames() {
        return Arbitraries.oneOf(
            Arbitraries.of(
                "Summer Music Festival",
                "Tech Conference 2024",
                "Comedy Night Live",
                "Sports Championship Final",
                "Rock Concert"
            ),
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50)
        );
    }

    private Arbitrary<String> tierNames() {
        return Arbitraries.of(
            "General Admission",
            "VIP",
            "Premium",
            "Early Bird",
            "Standard",
            "Gold",
            "Platinum"
        );
    }

    private Arbitrary<Long> bookingIds() {
        return Arbitraries.longs().between(1L, 50000L);
    }

    private Arbitrary<String> qrSecrets() {
        // Generate various qrSecret values to ensure they never leak into the DTO
        return Arbitraries.oneOf(
            Arbitraries.strings().alpha().numeric().ofMinLength(16).ofMaxLength(64),
            Arbitraries.of(
                "super-secret-qr-key-12345",
                "encrypted_qr_data_abc123",
                "qrSecret_should_never_appear"
            )
        );
    }

    /**
     * Helper to set private fields via reflection (needed for id and qrSecret which lack setters).
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
