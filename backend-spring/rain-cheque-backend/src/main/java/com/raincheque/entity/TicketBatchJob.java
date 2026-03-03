package com.raincheque.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_batch_jobs")
@Data
@NoArgsConstructor
public class TicketBatchJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_name", nullable = false)
    private String jobName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;
    
    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;
    
    @Column(name = "processed_tickets")
    private Integer processedTickets = 0;
    
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;
    
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}