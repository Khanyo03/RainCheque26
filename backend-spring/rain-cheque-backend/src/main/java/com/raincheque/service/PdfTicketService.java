package com.raincheque.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.raincheque.entity.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTicketService {
    
    private final QrCodeService qrCodeService;
    
    private static final float TICKET_WIDTH = 600;
    private static final float TICKET_HEIGHT = 250;
    
    /**
     * Generate single ticket PDF
     */
    public byte[] generateSingleTicket(Ticket ticket) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, new PageSize(TICKET_WIDTH, TICKET_HEIGHT));
            document.setMargins(10, 10, 10, 10);
            
            // Generate verification data for QR
            String verificationData = qrCodeService.generateTicketVerificationData(
                ticket.getId(),
                ticket.getTicketCode(),
                ticket.getQrSecret()
            );
            
            // Generate QR code
            String qrBase64 = qrCodeService.generateQrCodeBase64(verificationData);
            
            // Create ticket layout
            addTicketContent(document, ticket, qrBase64);
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generating PDF ticket", e);
            throw new RuntimeException("Failed to generate PDF ticket", e);
        }
    }
    
    /**
     * Generate batch tickets PDF (multiple tickets per page)
     */
    public byte[] generateBatchTickets(List<Ticket> tickets) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(20, 20, 20, 20);
            
            for (int i = 0; i < tickets.size(); i++) {
                if (i > 0) {
                    document.add(new Paragraph("\n"));
                }
                
                Ticket ticket = tickets.get(i);
                String verificationData = qrCodeService.generateTicketVerificationData(
                    ticket.getId(),
                    ticket.getTicketCode(),
                    ticket.getQrSecret()
                );
                String qrBase64 = qrCodeService.generateQrCodeBase64(verificationData);
                
                // Create a table for this ticket
                Table ticketTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setBorder(new SolidBorder(ColorConstants.BLACK, 1))
                    .setMarginBottom(10);
                
                // QR Code cell
                Cell qrCell = new Cell()
                    .add(createQrImage(document, qrBase64))
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER);
                ticketTable.addCell(qrCell);
                
                // Ticket details cell
                Cell detailsCell = new Cell()
                    .add(createTicketDetails(ticket))
                    .setBorder(Border.NO_BORDER)
                    .setPadding(10);
                ticketTable.addCell(detailsCell);
                
                document.add(ticketTable);
            }
            
            document.close();
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("Error generating batch PDF tickets", e);
            throw new RuntimeException("Failed to generate batch PDF tickets", e);
        }
    }
    
    private void addTicketContent(Document document, Ticket ticket, String qrBase64) 
            throws IOException {
        
        Table mainTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
            .setWidth(UnitValue.createPercentValue(100))
            .setHeight(UnitValue.createPercentValue(100));
        
        // Left side - QR Code
        Cell qrCell = new Cell()
            .add(createQrImage(document, qrBase64))
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);
        mainTable.addCell(qrCell);
        
        // Right side - Ticket details
        Cell detailsCell = new Cell()
            .add(createTicketDetails(ticket))
            .setBorder(Border.NO_BORDER)
            .setPadding(10);
        mainTable.addCell(detailsCell);
        
        document.add(mainTable);
    }
    
    private Image createQrImage(Document document, String qrBase64) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(
                qrBase64.replace("data:image/png;base64,", "")
            );
            Image qrImage = new Image(com.itextpdf.io.image.ImageDataFactory.create(imageBytes));
            qrImage.scaleToFit(120, 120);
            return qrImage;
        } catch (Exception e) {
            log.error("Error creating QR image", e);
            return null;
        }
    }
    
    private Table createTicketDetails(Ticket ticket) throws IOException {
        Table detailsTable = new Table(1);
        detailsTable.setWidth(UnitValue.createPercentValue(100));
        
        // Event name
        PdfFont boldFont = PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
        );
        
        Paragraph eventName = new Paragraph(ticket.getBooking().getEvent().getTitle())
            .setFont(boldFont)
            .setFontSize(16)
            .setMarginBottom(5);
        detailsTable.addCell(new Cell().add(eventName).setBorder(Border.NO_BORDER));
        
        // Ticket details grid
        Table grid = new Table(2);
        grid.setWidth(UnitValue.createPercentValue(100));
        
        // Add details
        addDetailRow(grid, "Ticket Code:", ticket.getTicketCode());
        addDetailRow(grid, "Ticket Type:", ticket.getTicketTier().getName());
        addDetailRow(grid, "Date:", ticket.getBooking().getEvent().getStartTime()
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")));
        addDetailRow(grid, "Venue:", ticket.getBooking().getEvent().getVenue().getName());
        addDetailRow(grid, "Seat:", ticket.getSeatNumber() != null ? ticket.getSeatNumber() : "General");
        addDetailRow(grid, "Price Paid:", "$" + ticket.getPricePaid());
        
        detailsTable.addCell(new Cell().add(grid).setBorder(Border.NO_BORDER).setPaddingTop(10));
        
        // Terms
        Paragraph terms = new Paragraph("Valid for one-time use. This ticket is non-transferable.")
            .setFontSize(8)
            .setFontColor(ColorConstants.GRAY)
            .setMarginTop(10);
        detailsTable.addCell(new Cell().add(terms).setBorder(Border.NO_BORDER));
        
        return detailsTable;
    }
    
    private void addDetailRow(Table table, String label, String value) {
        try {
            PdfFont normalFont = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA
            );
            PdfFont boldFont = PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
            );
            
            table.addCell(new Cell()
                .add(new Paragraph(label).setFont(normalFont).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(2));
            
            table.addCell(new Cell()
                .add(new Paragraph(value).setFont(boldFont).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(2)
                .setTextAlignment(TextAlignment.RIGHT));
        } catch (IOException e) {
            log.error("Error creating font", e);
        }
    }

}
    
