package com.raincheque.repository;

import com.raincheque.entity.Booking;
import com.raincheque.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find bookings by user ID
    List<Booking> findByUserId(Long userId);

    // Find bookings by event ID
    List<Booking> findByEventId(Long eventId);

    // Count bookings by status and event ID
    Long countByEventIdAndStatus(Long eventId, BookingStatus status);

    // Find bookings by status
    List<Booking> findByStatus(BookingStatus status);

    // Find bookings created after a certain date
    List<Booking> findByCreatedAtAfter(LocalDateTime createdAt);

    List<Booking> findConfirmedBookingsByEventId(Long eventId);
}
