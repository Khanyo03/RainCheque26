package com.raincheque.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_tier_id")
    private TicketTier ticketTier;
    
    @Column(name = "ticket_code", unique = true, nullable = false, length = 50)
    private String ticketCode;
    
    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;
    
    @Column(name = "qr_secret")
    private String qrSecret;
    
    @Column(name = "seat_number", length = 50)
    private String seatNumber;
    
    @Column(name = "price_paid", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.ACTIVE;
    
    @Column(name = "used_at")
    private LocalDateTime usedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    private User validatedBy;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TicketStatus {
        ACTIVE, USED, REFUNDED, CANCELLED
    }
    
    @PrePersist
    public void prePersist() {
        if (ticketCode == null) {
            ticketCode = generateTicketCode();
        }
        if (qrSecret == null) {
            qrSecret = UUID.randomUUID().toString();
        }
    }
    
    private String generateTicketCode() {
        // Format: EVT{eventId}-{timestamp}-{random}
        String eventPrefix = "EVT" + booking.getEvent().getId();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("%s-%s-%s", eventPrefix, timestamp, random);
    }
}