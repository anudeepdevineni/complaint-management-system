package com.project.complaintsystem.dto;

import com.project.complaintsystem.model.ComplaintHistory;

import java.time.LocalDateTime;

/**
 * One entry in a complaint's status-change history, flattened for the frontend.
 */
public class ComplaintHistoryResponse {

    public Long id;
    public String oldStatus;
    public String newStatus;
    public String changedBy;   // staff name, or "System" for auto-escalation
    public LocalDateTime changedAt;

    public static ComplaintHistoryResponse from(ComplaintHistory h) {
        ComplaintHistoryResponse r = new ComplaintHistoryResponse();
        r.id = h.getId();
        r.oldStatus = h.getOldStatus() != null ? h.getOldStatus().getName() : null;
        r.newStatus = h.getNewStatus() != null ? h.getNewStatus().getName() : null;
        r.changedBy = h.getChangedBy() != null ? h.getChangedBy().getName() : "System";
        r.changedAt = h.getChangedAt();
        return r;
    }
}
