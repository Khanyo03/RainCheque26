package com.raincheque.repository;

import com.raincheque.entity.Booking;
import com.raincheque.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    
    List<Booking> findByUserId(UUID userId);
    
    List<Booking> findByEventId(Long eventId);
    
    List<Booking> findByEventIdAndStatus(Long eventId, Booking.BookingStatus status);
    
    Optional<Booking> findByPaymentIntentId(String paymentIntentId);
    
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByUser(@Param("userId") UUID userId);
    
    @Query("SELECT e FROM Event e WHERE e.id = :eventId")
    Optional<Event> findEventById(@Param("eventId") Long eventId);
}