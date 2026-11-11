package com.project.complaintsystem.controller;

import com.project.complaintsystem.dto.ComplaintHistoryResponse;
import com.project.complaintsystem.dto.ComplaintResponse;
import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.service.ComplaintService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Complaint endpoints. Which role can call which endpoint is set in SecurityConfig;
 * here the logged-in user's email comes from auth.getName().
 */
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    // Submit complaint (STUDENT)
    @PostMapping
    public ComplaintResponse submitComplaint(@RequestBody Map<String, String> req,
                                             Authentication auth) {
        Complaint saved = complaintService.submitComplaint(
                req.get("title"),
                req.get("description"),
                auth.getName()
        );
        return ComplaintResponse.from(saved);
    }

    // Get all complaints (ADMIN)
    @GetMapping
    public List<ComplaintResponse> getAllComplaints() {
        return toResponses(complaintService.getAllComplaints());
    }

    // Get complaints by category (ADMIN)
    @GetMapping("/category/{category}")
    public List<ComplaintResponse> getComplaintsByCategory(@PathVariable String category) {
        return toResponses(complaintService.getComplaintsByCategory(category));
    }

    // Complaint counts per category (ADMIN)
    @GetMapping("/categories/summary")
    public Map<String, Long> getCategorySummary() {
        return complaintService.getCategorySummary();
    }

    // My complaints (STUDENT)
    @GetMapping("/my")
    public List<ComplaintResponse> getMyComplaints(Authentication auth) {
        return toResponses(complaintService.getMyComplaints(auth.getName()));
    }

    // Complaints assigned to me (STAFF)
    @GetMapping("/assigned")
    public List<ComplaintResponse> getAssignedComplaints(Authentication auth) {
        return toResponses(complaintService.getAssignedComplaints(auth.getName()));
    }

    // Update status of an assigned complaint (STAFF)
    @PutMapping("/status")
    public ComplaintResponse updateStatus(@RequestParam Long id,
                                          @RequestParam String status,
                                          Authentication auth) {
        Complaint updated = complaintService.updateStatus(id, status, auth.getName());
        return ComplaintResponse.from(updated);
    }

    // Status-change history for one complaint (ADMIN)
    @GetMapping("/{id}/history")
    public List<ComplaintHistoryResponse> getHistory(@PathVariable Long id) {
        return complaintService.getHistory(id).stream()
                .map(ComplaintHistoryResponse::from)
                .toList();
    }

    // Counts by status (ADMIN)
    @GetMapping("/analytics")
    public Map<String, Long> getAnalytics() {
        return complaintService.getAnalytics();
    }

    private List<ComplaintResponse> toResponses(List<Complaint> complaints) {
        return complaints.stream().map(ComplaintResponse::from).toList();
    }
}
