package com.raincheque.repository;

import com.raincheque.entity.TicketBatchJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketBatchJobRepository extends JpaRepository<TicketBatchJob, Long> {
    
    List<TicketBatchJob> findByEvent_Id(Long eventId);
    
    List<TicketBatchJob> findByCreatedBy_Id(Long userId);
    
    List<TicketBatchJob> findByStatus(TicketBatchJob.JobStatus status);
}