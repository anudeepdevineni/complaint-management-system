package com.project.complaintsystem.scheduler;

import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.model.ComplaintHistory;
import com.project.complaintsystem.model.Status;
import com.project.complaintsystem.repository.ComplaintHistoryRepository;
import com.project.complaintsystem.repository.ComplaintRepository;
import com.project.complaintsystem.repository.StatusRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EscalationScheduler {

    private final ComplaintRepository complaintRepository;
    private final StatusRepository statusRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;

    public EscalationScheduler(ComplaintRepository complaintRepository,
                               StatusRepository statusRepository,
                               ComplaintHistoryRepository complaintHistoryRepository) {
        this.complaintRepository = complaintRepository;
        this.statusRepository = statusRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
    }

    @Scheduled(fixedRate = 3600000) // runs every hour
    public void checkAndEscalate() {

        Status escalatedStatus = statusRepository.findByName("ESCALATED")
                .orElseThrow(() -> new RuntimeException("ESCALATED status not found in DB"));

        List<Complaint> complaints = complaintRepository.findAll();

        for (Complaint c : complaints) {

            //  null-safe status check — avoids NullPointerException
            if (c.getStatus() == null) continue;
            if (c.getCreatedAt() == null) continue;

            // Skip already resolved or already escalated complaints
            String statusName = c.getStatus().getName();
            if (statusName.equals("RESOLVED") || statusName.equals("ESCALATED") || c.isEscalated()) continue;

            // Escalate if older than 1 hour and still not resolved
            if (c.getCreatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                Status oldStatus = c.getStatus();

                c.setStatus(escalatedStatus);
                c.setEscalated(true);
                complaintRepository.save(c);

                // Record the auto-escalation in the history (changedBy = null = System).
                ComplaintHistory history = new ComplaintHistory();
                history.setComplaint(c);
                history.setOldStatus(oldStatus);
                history.setNewStatus(escalatedStatus);
                history.setChangedBy(null);
                complaintHistoryRepository.save(history);

                System.out.println("Escalated complaint ID: " + c.getId());
            }
        }
    }
}