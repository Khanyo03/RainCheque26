package com.raincheque.service;

import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketStatus;
import com.raincheque.entity.TicketBatchJob;
import com.raincheque.entity.User;
import com.raincheque.exception.ResourceNotFoundException;
import com.raincheque.repository.TicketBatchJobRepository;
import com.raincheque.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
        this.batchJobRepository = null;
    }

    @Transactional
    public void markAsUsed(Long ticketId, Long validatorId, LocalDateTime usedAt) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(usedAt);
        ticket.setValidatedBy(new User(validatorId));

        ticketRepository.save(ticket);
    }

    private final TicketBatchJobRepository batchJobRepository;

    // =========================
    // BASIC QUERIES
    // =========================

    public List<Ticket> getUserTickets(Long userId) {
        log.debug("Fetching tickets for user {}", userId);
        return ticketRepository.findByUserId(userId);
    }

    public Ticket getTicket(Long ticketId) {
        log.debug("Fetching ticket {}", ticketId);
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("Ticket {} not found", ticketId);
                    return new ResourceNotFoundException("Ticket not found");
                });
    }

    public List<Ticket> getBookingTickets(Long bookingId) {
        log.debug("Fetching tickets for booking {}", bookingId);
        return ticketRepository.findByBookingId(bookingId);
    }

    // =========================
    // VALIDATION
    // =========================

    @Transactional
    public Ticket validateTicket(String ticketCode, User validator) {
        log.info("Validating ticket {} by user {}", ticketCode, validator.getId());

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> {
                    log.warn("Ticket {} not found", ticketCode);
                    return new ResourceNotFoundException("Ticket not found");
                });

        // Compare status by name to avoid direct reference to a possibly non-existent enum constant
        if (ticket.getStatus() == null || !"ACTIVE".equals(ticket.getStatus().name())) {
            log.warn("Attempt to validate inactive ticket {} by user {}", ticket.getId(), validator.getId());
            throw new IllegalStateException("Ticket is not active: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticket.setValidatedBy(validator);

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} successfully validated by user {}", saved.getId(), validator.getId());
        return saved;
    }

    // =========================
    // BATCH JOB
    // =========================

    public TicketBatchJob getBatchJob(Long jobId) {
        log.debug("Fetching batch job {}", jobId);
        return batchJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Batch job {} not found", jobId);
                    return new ResourceNotFoundException("Batch job not found");
                });
    }

    // =========================
    // SECURITY METHODS
    // =========================

    public Ticket getTicketForUser(Long ticketId, Long userId) {
        Ticket ticket = getTicket(ticketId);

        if (!ticket.getBooking().getUser().getId().equals(userId)) {
            log.warn("Unauthorized access attempt for ticket {} by user {}", ticketId, userId);
            throw new ResourceNotFoundException("Ticket not found");
        }

        return ticket;
    }

    public List<Ticket> getBookingTicketsForUser(Long bookingId, Long userId) {
        List<Ticket> tickets = getBookingTickets(bookingId);

        if (tickets.isEmpty()) {
            log.debug("No tickets found for booking {}", bookingId);
            return tickets;
        }

        Long ownerId = tickets.get(0).getBooking().getUser().getId();

        if (!ownerId.equals(userId)) {
            log.warn("Unauthorized access attempt for booking {} by user {}", bookingId, userId);
            throw new ResourceNotFoundException("Booking not found");
        }

        return tickets;
    }

    public TicketBatchJob getBatchJobForUser(Long jobId, Long userId) {
        TicketBatchJob job = getBatchJob(jobId);

        if (!job.getCreatedBy().getId().equals(userId)) {
            log.warn("Unauthorized access attempt for batch job {} by user {}", jobId, userId);
            throw new ResourceNotFoundException("Batch job not found");
        }

        return job;
    }

    

    
}
