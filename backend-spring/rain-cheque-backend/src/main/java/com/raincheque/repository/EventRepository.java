package com.raincheque.repository;

import com.raincheque.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    
    Page<Event> findByStatus(Event.EventStatus status, Pageable pageable);
    
    List<Event> findByStatusAndFeatured(Event.EventStatus status, boolean featured, Pageable pageable);
    
    List<Event> findByVenueIdAndStartTimeBetween(Long venueId, LocalDateTime start, LocalDateTime end);
    
    List<Event> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);
    
    Page<Event> findByCategoryAndStatus(String category, Event.EventStatus status, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.startTime > :now ORDER BY e.startTime ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE " +
           "LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.venue.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Event> searchEvents(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT DISTINCT e.category FROM Event e WHERE e.category IS NOT NULL")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT e.category FROM Event e WHERE e.status = :status AND e.category IS NOT NULL")
    List<String> findDistinctCategoriesByStatus(@Param("status") Event.EventStatus status);
    
    @Query("SELECT DISTINCT e.venue.city FROM Event e WHERE e.venue.city IS NOT NULL")
    List<String> findAllCities();
    
    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' ORDER BY SIZE(e.ticketTiers) DESC")
    List<Event> findPopularEvents(Pageable pageable);
    
    @Query(value = "SELECT DISTINCT e.* FROM events e " +
           "JOIN bookings b ON e.id = b.event_id " +
           "WHERE b.user_id = :userId AND e.status = 'PUBLISHED' " +
           "ORDER BY e.start_time DESC", nativeQuery = true)
    List<Event> findRecommendedEvents(@Param("userId") Long userId, Pageable pageable);
}