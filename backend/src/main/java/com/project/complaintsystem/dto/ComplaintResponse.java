package com.project.complaintsystem.dto;

import com.project.complaintsystem.model.Complaint;

import java.time.LocalDateTime;

/**
 * The shape of a complaint as sent to the frontend.
 *
 * We return this instead of the raw Complaint entity so the API is flat and
 * predictable (category/priority/status are plain strings, not nested objects)
 * and so we never accidentally leak fields like the student's password.
 */
public class ComplaintResponse {

    public Long id;
    public String title;
    public String description;
    public String category;
    public String priority;
    public String status;
    public String studentName;
    public String assignedStaffName;
    public String assignedStaffDepartment;
    public LocalDateTime createdAt;
    public boolean escalated;

    // Builds a response object out of a Complaint entity, flattening the lookups.
    public static ComplaintResponse from(Complaint c) {
        ComplaintResponse r = new ComplaintResponse();
        r.id = c.getId();
        r.title = c.getTitle();
        r.description = c.getDescription();
        r.category = c.getCategory() != null ? c.getCategory().getName() : null;
        r.priority = c.getPriority() != null ? c.getPriority().getName() : null;
        r.status = c.getStatus() != null ? c.getStatus().getName() : null;
        r.studentName = c.getStudent() != null ? c.getStudent().getName() : null;

        if (c.getAssignedStaff() != null) {
            r.assignedStaffName = c.getAssignedStaff().getName();
            r.assignedStaffDepartment = c.getAssignedStaff().getDepartment() != null
                    ? c.getAssignedStaff().getDepartment().getName()
                    : null;
        } else {
            r.assignedStaffName = null;   // frontend shows "Not assigned"
            r.assignedStaffDepartment = null;
        }

        r.createdAt = c.getCreatedAt();
        r.escalated = c.isEscalated();
        return r;
    }
}
