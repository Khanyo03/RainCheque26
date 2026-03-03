package com.raincheque.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ticket_tiers")
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TicketTier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;
    
    @Column(name = "max_per_user")
    private Integer maxPerUser = 10;
    
    @Column(name = "min_per_user")
    private Integer minPerUser = 1;
    
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> benefits;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "sales_start")
    private LocalDateTime salesStart;
    
    @Column(name = "sales_end")
    private LocalDateTime salesEnd;
    
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}