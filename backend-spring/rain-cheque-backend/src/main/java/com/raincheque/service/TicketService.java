package com.raincheque.service;

import com.raincheque.entity.Ticket;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final TicketBatchJobRepository batchJobRepository;
    
    public List<Ticket> getUserTickets(UUID userId) {
        return ticketRepository.findByUserId(userId);
    }
    
    public Ticket getTicket(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }
    
    public List<Ticket> getBookingTickets(UUID bookingId) {
        return ticketRepository.findByBookingId(bookingId);
    }
    
    @Transactional
    public Ticket validateTicket(String ticketCode, User validator) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        
        if (ticket.getStatus() != Ticket.TicketStatus.ACTIVE) {
            throw new IllegalStateException("Ticket is not active. Current status: " + ticket.getStatus());
        }
        
        ticket.setStatus(Ticket.TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticket.setValidatedBy(validator);
        
        return ticketRepository.save(ticket);
    }
    
    public TicketBatchJob getBatchJob(Long jobId) {
        return batchJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch job not found"));
    }
}