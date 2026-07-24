package com.raincheque.repository;

import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketStatus; // assuming you have an enum for status
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByBookingId(Long bookingId);

    @Query("SELECT t FROM Ticket t WHERE t.booking.user.id = :userId")
    List<Ticket> findByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Ticket t WHERE t.booking.event.id = :eventId")
    List<Ticket> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.booking.event.id = :eventId AND t.status = :status")
    Long countTicketsByEventAndStatus(@Param("eventId") Long eventId,
                                      @Param("status") TicketStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE Ticket t SET t.status = :newStatus, t.usedAt = :usedAt, t.validatedBy.id = :validatorId " +
           "WHERE t.id = :ticketId AND t.status = :expectedStatus")
    int markAsUsed(@Param("ticketId") Long ticketId,
                   @Param("validatorId") Long validatorId,
                   @Param("usedAt") LocalDateTime usedAt,
                   @Param("newStatus") TicketStatus newStatus,
                   @Param("expectedStatus") TicketStatus expectedStatus);

}
