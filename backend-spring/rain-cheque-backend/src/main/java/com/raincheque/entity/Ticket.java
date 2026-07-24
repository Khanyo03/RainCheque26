package com.raincheque.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;

@Entity
@Table(name = "tickets")
public class Ticket {

    public static final String TicketStatus = null;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticketCode;

    @Enumerated(EnumType.STRING)   // ensures values like "ACTIVE" are stored
    @Column(nullable = false)
    private TicketStatus status;

    @Column
    private LocalDateTime usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id", nullable = false)
    private TicketTier ticketTier;

    @Column(name = "price_paid", nullable = false)
    private BigDecimal pricePaid;

    @Column(name = "qr_secret", nullable = false)
    private String qrSecret;

    @Column(name = "seat_number")
    private String seatNumber;

    // getters and setters

    public Long getId() {
        return id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public User getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(User validatedBy) {
        this.validatedBy = validatedBy;
    }

    public void setTicketTier(TicketTier ticketTier) {
        this.ticketTier = ticketTier;
    }

    public void setPricePaid(BigDecimal price) {
        this.pricePaid = price;
    }

    public void setSeatNumber(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSeatNumber'");
    }

    public TicketTier getTicketTier() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTicketTier'");
    }

    public String getSeatNumber() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSeatNumber'");
    }

    public BigDecimal getPricePaid() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPricePaid'");
    }

    public String getQrSecret() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQrSecret'");
    }
}
