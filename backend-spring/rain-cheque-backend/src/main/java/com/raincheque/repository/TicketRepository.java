package com.raincheque.repository;

import com.raincheque.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    Optional<Ticket> findByTicketCode(String ticketCode);
    
    List<Ticket> findByBookingId(UUID bookingId);
    
    @Query("SELECT t FROM Ticket t WHERE t.booking.user.id = :userId")
    List<Ticket> findByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT t FROM Ticket t WHERE t.booking.event.id = :eventId")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.booking.event.id = :eventId AND t.status = 'ACTIVE'")
    Long countActiveTicketsByEvent(@Param("eventId") Long eventId);
    
    @Modifying
    @Query("UPDATE Ticket t SET t.status = 'USED', t.usedAt = :usedAt, t.validatedBy.id = :validatorId WHERE t.id = :ticketId AND t.status = 'ACTIVE'")
    int markAsUsed(@Param("ticketId") Long ticketId, @Param("validatorId") UUID validatorId, @Param("usedAt") LocalDateTime usedAt);
}