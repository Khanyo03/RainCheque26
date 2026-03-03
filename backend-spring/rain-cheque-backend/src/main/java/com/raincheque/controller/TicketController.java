package com.raincheque.controller;

import com.raincheque.entity.Ticket;
import com.raincheque.entity.TicketBatchJob;
import com.raincheque.entity.User;
import com.raincheque.service.BatchTicketService;
import com.raincheque.service.PdfTicketService;
import com.raincheque.service.QrCodeService;
import com.raincheque.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    
    private final TicketService ticketService;
    private final BatchTicketService batchTicketService;
    private final PdfTicketService pdfTicketService;
    private final QrCodeService qrCodeService;
    
    /**
     * Get user's tickets
     */
    @GetMapping
    public ResponseEntity<List<Ticket>> getUserTickets(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketService.getUserTickets(user.getId()));
    }
    
    /**
     * Get specific ticket
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicket(ticketId));
    }
    
    /**
     * Download single ticket PDF
     */
    @GetMapping("/{ticketId}/download")
    public ResponseEntity<Resource> downloadTicket(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicket(ticketId);
        byte[] pdfBytes = pdfTicketService.generateSingleTicket(ticket);
        
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=ticket-" + ticket.getTicketCode() + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.length)
            .body(resource);
    }
    
    /**
     * Download batch tickets for a booking
     */
    @GetMapping("/booking/{bookingId}/download")
    public ResponseEntity<Resource> downloadBookingTickets(
            @PathVariable UUID bookingId) {
        List<Ticket> tickets = ticketService.getBookingTickets(bookingId);
        byte[] pdfBytes = pdfTicketService.generateBatchTickets(tickets);
        
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=booking-tickets-" + bookingId + ".pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(pdfBytes.length)
            .body(resource);
    }
    
    /**
     * Validate a ticket (for venue staff)
     */
    @PostMapping("/validate")
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @RequestBody TicketValidationRequest request,
            @AuthenticationPrincipal User validator) {
        
        boolean isValid = qrCodeService.verifyTicket(request.qrData());
        
        if (!isValid) {
            return ResponseEntity.badRequest().body(
                new TicketValidationResponse(false, "Invalid QR code", null)
            );
        }
        
        Ticket ticket = ticketService.validateTicket(request.ticketCode(), validator);
        
        return ResponseEntity.ok(new TicketValidationResponse(
            true,
            "Ticket validated successfully",
            ticket
        ));
    }
    
    /**
     * Start batch ticket generation for event
     */
    @PostMapping("/batch/event/{eventId}")
    public ResponseEntity<TicketBatchJob> startBatchGeneration(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User user) {
        
        TicketBatchJob job = batchTicketService.batchGenerateEventTickets(eventId, user.getId())
            .join();
        
        return ResponseEntity.accepted().body(job);
    }
    
    /**
     * Get batch job status
     */
    @GetMapping("/batch/job/{jobId}")
    public ResponseEntity<TicketBatchJob> getBatchJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(ticketService.getBatchJob(jobId));
    }
    
    // Request/Response DTOs
    public record TicketValidationRequest(String ticketCode, String qrData) {}
    public record TicketValidationResponse(boolean valid, String message, Ticket ticket) {}
}