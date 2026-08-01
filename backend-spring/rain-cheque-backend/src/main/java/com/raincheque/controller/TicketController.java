package com.raincheque.controller;

import com.raincheque.dto.TicketResponse;
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

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final BatchTicketService batchTicketService;
    private final PdfTicketService pdfTicketService;
    private final QrCodeService qrCodeService;

    // ===================== USER TICKETS =====================

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getUserTickets(
            @AuthenticationPrincipal User user) {

        List<TicketResponse> tickets = ticketService.getUserTickets(user.getId())
                .stream()
                .map(TicketResponse::from)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    // ===================== SINGLE TICKET =====================

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {

        Ticket ticket = ticketService.getTicketForUser(ticketId, user.getId());
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    // ===================== DOWNLOAD SINGLE =====================

    @GetMapping("/{ticketId}/download")
    public ResponseEntity<Resource> downloadTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal User user) {

        Ticket ticket = ticketService.getTicketForUser(ticketId, user.getId());

        byte[] pdfBytes = pdfTicketService.generateSingleTicket(ticket);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + ticket.getTicketCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    // ===================== BOOKING TICKETS =====================

    @GetMapping("/booking/{bookingId}/download")
    public ResponseEntity<Resource> downloadBookingTickets(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal User user) {

        List<Ticket> tickets =
                ticketService.getBookingTicketsForUser(bookingId, user.getId());

        byte[] pdfBytes = pdfTicketService.generateBatchTickets(tickets);

        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=booking-tickets-" + bookingId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(resource);
    }

    // ===================== VALIDATION =====================

    @PostMapping("/validate")
    public ResponseEntity<TicketValidationResponse> validateTicket(
            @RequestBody TicketValidationRequest request,
            @AuthenticationPrincipal User validator) {

        boolean isValid = qrCodeService.verifyTicket(request.qrData());

        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(new TicketValidationResponse(
                            false,
                            "Invalid QR code",
                            null
                    ));
        }

        Ticket ticket = ticketService.validateTicket(
                request.ticketCode(),
                validator
        );

        return ResponseEntity.ok(
                new TicketValidationResponse(
                        true,
                        "Ticket validated successfully",
                        TicketResponse.from(ticket)
                )
        );
    }

    // ===================== BATCH START (FIXED ASYNC) =====================

    @PostMapping("/batch/event/{eventId}")
    public ResponseEntity<String> startBatchGeneration(
            @PathVariable Long eventId,
            @AuthenticationPrincipal User user) {

        batchTicketService.batchGenerateEventTickets(
                eventId,
                user.getId()
        );

        return ResponseEntity.accepted()
                .body("Batch ticket generation started");
    }

    // ===================== JOB STATUS =====================

    @GetMapping("/batch/job/{jobId}")
    public ResponseEntity<TicketBatchJob> getBatchJobStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                ticketService.getBatchJobForUser(jobId, user.getId())
        );
    }

    // ================= DTOs =================

    public record TicketValidationRequest(
            String ticketCode,
            String qrData
    ) {}

    public record TicketValidationResponse(
            boolean valid,
            String message,
            TicketResponse ticket
    ) {}
}