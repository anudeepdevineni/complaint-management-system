package com.project.complaintsystem.controller;

import com.project.complaintsystem.dto.ComplaintHistoryResponse;
import com.project.complaintsystem.dto.ComplaintResponse;
import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.security.JwtUtil;
import com.project.complaintsystem.service.ComplaintService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final JwtUtil jwtUtil;

    public ComplaintController(ComplaintService complaintService, JwtUtil jwtUtil) {
        this.complaintService = complaintService;
        this.jwtUtil = jwtUtil;
    }

    // Submit complaint (STUDENT)
    @PostMapping
    public ComplaintResponse submitComplaint(
            @RequestBody Map<String, String> req,
            @RequestHeader("Authorization") String header
    ) {
        String email = jwtUtil.extractEmail(tokenOf(header));

        Complaint saved = complaintService.submitComplaint(
                req.get("title"),
                req.get("description"),
                email
        );
        return ComplaintResponse.from(saved);
    }

    // Get all complaints (ADMIN ONLY)
    @GetMapping
    public List<ComplaintResponse> getAllComplaints(
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "ADMIN");
        return toResponses(complaintService.getAllComplaints());
    }

    // Get complaints by category (ADMIN ONLY)
    @GetMapping("/category/{category}")
    public List<ComplaintResponse> getComplaintsByCategory(
            @PathVariable String category,
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "ADMIN");
        return toResponses(complaintService.getComplaintsByCategory(category));
    }

    // Get all categories with complaint counts (ADMIN ONLY)
    @GetMapping("/categories/summary")
    public Map<String, Long> getCategorySummary(
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "ADMIN");
        return complaintService.getCategorySummary();
    }

    // Get my complaints (STUDENT)
    @GetMapping("/my")
    public List<ComplaintResponse> getMyComplaints(
            @RequestHeader("Authorization") String header
    ) {
        String email = jwtUtil.extractEmail(tokenOf(header));
        return toResponses(complaintService.getMyComplaints(email));
    }

    // Get assigned complaints (STAFF ONLY)
    @GetMapping("/assigned")
    public List<ComplaintResponse> getAssignedComplaints(
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "STAFF");
        String staffEmail = jwtUtil.extractEmail(tokenOf(header));
        return toResponses(complaintService.getAssignedComplaints(staffEmail));
    }

    // Update status (STAFF ONLY - assigned complaints)
    @PutMapping("/status")
    public ComplaintResponse updateStatus(
            @RequestParam Long id,
            @RequestParam String status,
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "STAFF");
        String staffEmail = jwtUtil.extractEmail(tokenOf(header));
        Complaint updated = complaintService.updateStatus(id, status, staffEmail);
        return ComplaintResponse.from(updated);
    }

    // Status-change history for one complaint (ADMIN ONLY)
    @GetMapping("/{id}/history")
    public List<ComplaintHistoryResponse> getHistory(
            @PathVariable Long id,
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "ADMIN");
        return complaintService.getHistory(id).stream()
                .map(ComplaintHistoryResponse::from)
                .toList();
    }

    // Analytics (ADMIN ONLY)
    @GetMapping("/analytics")
    public Map<String, Long> getAnalytics(
            @RequestHeader("Authorization") String header
    ) {
        requireRole(header, "ADMIN");
        return complaintService.getAnalytics();
    }

    // ===== small helpers to keep the endpoints above tidy =====

    // Strips the "Bearer " prefix off the Authorization header.
    private String tokenOf(String header) {
        return header.substring(7);
    }

    // Reads the role from the token and blocks the request if it doesn't match.
    private void requireRole(String header, String requiredRole) {
        String role = jwtUtil.extractRole(tokenOf(header));
        if (!requiredRole.equals(role)) {
            throw new RuntimeException("Only " + requiredRole.toLowerCase() + " allowed");
        }
    }

    private List<ComplaintResponse> toResponses(List<Complaint> complaints) {
        return complaints.stream().map(ComplaintResponse::from).toList();
    }
}
