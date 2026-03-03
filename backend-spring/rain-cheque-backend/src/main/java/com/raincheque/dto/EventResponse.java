package com.raincheque.dto;

import com.raincheque.entity.Event;
import com.raincheque.entity.Event.EventMetadata;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private String shortDescription;

    private String venueName;
    private String venueCity;
    private String venueAddress;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String category;
    private String bannerImageUrl;
    private String thumbnailUrl;

    private Event.EventStatus status;
    private boolean featured;

    private List<TicketTierDto> ticketTiers;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    // ✅ FIXED TYPE
    private EventMetadata metadata;

    private Integer totalAvailableTickets;

    @Data
    public static class TicketTierDto {

        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private Integer remainingQuantity;
        private Integer totalQuantity;
        private Integer maxPerUser;
        private Integer minPerUser;
        private List<String> benefits;

        // ✅ FIXED FIELD NAME
        private Boolean active;
    }
}