package com.project.complaintsystem.scheduler;

import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.model.ComplaintHistory;
import com.project.complaintsystem.model.Status;
import com.project.complaintsystem.repository.ComplaintHistoryRepository;
import com.project.complaintsystem.repository.ComplaintRepository;
import com.project.complaintsystem.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EscalationScheduler {

    private final ComplaintRepository complaintRepository;
    private final StatusRepository statusRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;

    // Unresolved complaints older than this are escalated (default 48 hours)
    @Value("${escalation.after-hours:48}")
    private long escalateAfterHours;

    public EscalationScheduler(ComplaintRepository complaintRepository,
                               StatusRepository statusRepository,
                               ComplaintHistoryRepository complaintHistoryRepository) {
        this.complaintRepository = complaintRepository;
        this.statusRepository = statusRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
    }

    // Checks every hour by default
    // First run waits a minute so DataSeeder has created the statuses
    @Scheduled(initialDelay = 60000, fixedRateString = "${escalation.check-interval-ms:3600000}")
    public void checkAndEscalate() {

        Status escalatedStatus = statusRepository.findByName("ESCALATED")
                .orElseThrow(() -> new RuntimeException("ESCALATED status not found in DB"));

        List<Complaint> complaints = complaintRepository.findAll();

        for (Complaint c : complaints) {

            // Skip rows with missing data
            if (c.getStatus() == null) continue;
            if (c.getCreatedAt() == null) continue;

            // Skip already resolved or already escalated complaints
            String statusName = c.getStatus().getName();
            if (statusName.equals("RESOLVED") || statusName.equals("ESCALATED") || c.isEscalated()) continue;

            // Escalate if it has been open longer than the limit
            if (c.getCreatedAt().isBefore(LocalDateTime.now().minusHours(escalateAfterHours))) {
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