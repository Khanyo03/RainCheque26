package com.raincheque.service;

import com.raincheque.entity.*;
import com.raincheque.repository.BookingRepository;
import com.raincheque.repository.TicketBatchJobRepository;
import com.raincheque.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchTicketService {
    
    private final TicketRepository ticketRepository;
    private final BookingRepository bookingRepository;
    private final TicketBatchJobRepository batchJobRepository;
    private final PdfTicketService pdfTicketService;
    private final EmailService emailService;
    
    /**
     * Generate tickets for a confirmed booking
     */
    @Transactional
    public List<Ticket> generateTicketsForBooking(Booking booking) {
        List<Ticket> tickets = new ArrayList<>();
        
        for (int i = 0; i < booking.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setTicketTier(booking.getTicketTier());
            ticket.setPricePaid(booking.getTicketTier().getPrice());
            ticket.setStatus(Ticket.TicketStatus.ACTIVE);
            
            // Generate seat number if applicable
            if (booking.getSeatNumbers() != null && i < booking.getSeatNumbers().size()) {
                ticket.setSeatNumber(booking.getSeatNumbers().get(i));
            }
            
            tickets.add(ticketRepository.save(ticket));
        }
        
        // Generate PDF and send email
        generateAndSendTickets(tickets, booking.getUser().getEmail());
        
        return tickets;
    }
    
    /**
     * Batch generate tickets for an event
     */
    @Async
    @Transactional
    public CompletableFuture<TicketBatchJob> batchGenerateEventTickets(
            Long eventId, UUID userId) {
        
        TicketBatchJob job = new TicketBatchJob();
        job.setJobName("Event Ticket Generation - " + eventId);
        
        // We need to get the event - this would be injected via EventRepository
        // For now, create a placeholder
        Event event = new Event();
        event.setId(eventId);
        job.setEvent(event);
        
        job.setTotalTickets(0);
        job.setStatus(TicketBatchJob.JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        
        User user = new User();
        user.setId(userId);
        job.setCreatedBy(user);
        
        job.setCreatedAt(LocalDateTime.now());
        
        job = batchJobRepository.save(job);
        
        try {
            // Find all confirmed bookings for this event
            List<Booking> confirmedBookings = bookingRepository
                .findByEventIdAndStatus(eventId, Booking.BookingStatus.CONFIRMED);
            
            int processed = 0;
            int totalTickets = 0;
            
            for (Booking booking : confirmedBookings) {
                List<Ticket> existingTickets = ticketRepository.findByBookingId(booking.getId());
                
                // Generate missing tickets
                if (existingTickets.size() < booking.getQuantity()) {
                    int ticketsToGenerate = booking.getQuantity() - existingTickets.size();
                    
                    for (int i = 0; i < ticketsToGenerate; i++) {
                        Ticket ticket = new Ticket();
                        ticket.setBooking(booking);
                        ticket.setTicketTier(booking.getTicketTier());
                        ticket.setPricePaid(booking.getTicketTier().getPrice());
                        ticket.setStatus(Ticket.TicketStatus.ACTIVE);
                        
                        ticketRepository.save(ticket);
                        totalTickets++;
                    }
                } else {
                    totalTickets += existingTickets.size();
                }
                
                processed++;
                
                // Update job progress every 10 bookings
                if (processed % 10 == 0) {
                    job.setProcessedTickets(totalTickets);
                    batchJobRepository.save(job);
                }
            }
            
            job.setProcessedTickets(totalTickets);
            job.setTotalTickets(totalTickets);
            job.setStatus(TicketBatchJob.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Error in batch ticket generation", e);
            job.setStatus(TicketBatchJob.JobStatus.FAILED);
            job.setErrorLog(e.getMessage());
        }
        
        job = batchJobRepository.save(job);
        return CompletableFuture.completedFuture(job);
    }
    
    /**
     * Generate ticket PDFs and send via email
     */
    @Async
    public void generateAndSendTickets(List<Ticket> tickets, String recipientEmail) {
        try {
            // Generate batch PDF
            byte[] pdfBytes = pdfTicketService.generateBatchTickets(tickets);
            
            // Send email with attachment
            String subject = "Your Rain Cheque Tickets";
            String body = "<h1>Thank you for your purchase!</h1><p>Your tickets are attached to this email.</p>";
            
            emailService.sendEmailWithAttachment(
                recipientEmail,
                subject,
                body,
                pdfBytes,
                "tickets.pdf"
            );
            
            log.info("Tickets sent to: {}", recipientEmail);
            
        } catch (Exception e) {
            log.error("Error sending tickets email", e);
        }
    }
}