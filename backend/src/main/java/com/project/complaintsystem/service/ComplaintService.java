package com.project.complaintsystem.service;

import com.project.complaintsystem.model.*;
import com.project.complaintsystem.repository.*;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ComplaintService {

    // Which status a complaint is allowed to move to, keyed by its current status.
    // Any move not listed here is rejected as an invalid transition.
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "OPEN",        Set.of("IN_PROGRESS"),
            "IN_PROGRESS", Set.of("RESOLVED"),
            "ESCALATED",   Set.of("IN_PROGRESS"),
            "RESOLVED",    Set.of("OPEN")   // allow reopening
    );

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository;

    private final AiService aiService;

    public ComplaintService(ComplaintRepository complaintRepository,
                            UserRepository userRepository,
                            CategoryRepository categoryRepository,
                            PriorityRepository priorityRepository,
                            StatusRepository statusRepository,
                            ComplaintHistoryRepository complaintHistoryRepository,
                            AiService aiService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.complaintHistoryRepository = complaintHistoryRepository;
        this.aiService = aiService;
    }

    // ================= SUBMIT =================
    public Complaint submitComplaint(String title, String description, String email) {

        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, String> ai = aiService.analyzeComplaint(description);

        Category category = categoryRepository.findByName(ai.get("category"))
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Priority priority = priorityRepository.findByName(ai.get("priority"))
                .orElseThrow(() -> new RuntimeException("Priority not found"));

        Status status = statusRepository.findByName("OPEN")
                .orElseThrow(() -> new RuntimeException("Status not found"));

        Complaint complaint = new Complaint();
        complaint.setTitle(title);
        complaint.setDescription(description);
        complaint.setStudent(student);
        complaint.setCategory(category);
        complaint.setPriority(priority);
        complaint.setStatus(status);

        // Only auto-assign when the category maps to a department (e.g. WIFI, HOSTEL).
        // GENERAL has no department, so it stays unassigned instead of being handed
        // to a random user. Without this guard, findByDepartment(null) would match
        // every user that has no department (all students + admin) and pick one.
        if (category.getDepartment() != null) {
            List<User> staffList = userRepository.findByDepartment(category.getDepartment());

            if (!staffList.isEmpty()) {
                // Pick the worker in this department with the fewest ACTIVE complaints
                // (anything not yet RESOLVED), so work is spread evenly.
                User leastLoaded = staffList.stream()
                        .min(Comparator.comparingLong(staff ->
                                complaintRepository.countByAssignedStaffAndStatus_NameNot(staff, "RESOLVED")
                        ))
                        .orElse(staffList.get(0));

                complaint.setAssignedStaff(leastLoaded);
            }
        }

        return complaintRepository.save(complaint);
    }

    // ================= GET =================
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public List<Complaint> getMyComplaints(String email) {
        User student = userRepository.findByEmail(email).orElseThrow();
        return complaintRepository.findByStudent(student);
    }

    public List<Complaint> getAssignedComplaints(String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail).orElseThrow();
        return complaintRepository.findByAssignedStaff(staff);
    }

    // ================= UPDATE STATUS =================
    public Complaint updateStatus(Long id, String newStatus, String staffEmail) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        User staff = userRepository.findByEmail(staffEmail).orElseThrow();

        if (complaint.getAssignedStaff() == null ||
                !complaint.getAssignedStaff().getId().equals(staff.getId())) {
            throw new RuntimeException("Not your complaint");
        }

        Status oldStatus = complaint.getStatus();
        String from = oldStatus != null ? oldStatus.getName() : null;
        String to = newStatus.toUpperCase();

        Status status = statusRepository.findByName(to)
                .orElseThrow(() -> new RuntimeException("Invalid status: " + newStatus));

        // Reject illegal jumps like OPEN -> RESOLVED.
        if (from == null || !ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) {
            throw new RuntimeException("Invalid status transition from " + from + " to " + to);
        }

        complaint.setStatus(status);
        Complaint saved = complaintRepository.save(complaint);

        recordHistory(saved, oldStatus, status, staff);

        return saved;
    }

    // Saves one row describing a status change, for the audit trail.
    private void recordHistory(Complaint complaint, Status oldStatus, Status newStatus, User changedBy) {
        ComplaintHistory history = new ComplaintHistory();
        history.setComplaint(complaint);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);   // null == changed by the system (auto-escalation)
        complaintHistoryRepository.save(history);
    }

    // Full status-change history for one complaint (used by the admin history endpoint).
    public List<ComplaintHistory> getHistory(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        return complaintHistoryRepository.findByComplaintOrderByChangedAtAsc(complaint);
    }

    // ================= ANALYTICS =================
    public Map<String, Long> getAnalytics() {

        Map<String, Long> data = new HashMap<>();

        data.put("total", complaintRepository.count());
        data.put("open", complaintRepository.countByStatus_Name("OPEN"));
        data.put("in_progress", complaintRepository.countByStatus_Name("IN_PROGRESS"));
        data.put("resolved", complaintRepository.countByStatus_Name("RESOLVED"));
        data.put("escalated", complaintRepository.countByStatus_Name("ESCALATED"));

        return data;
    }

    public Map<String, Long> getCategorySummary() {

        Map<String, Long> map = new HashMap<>();

        map.put("WIFI", complaintRepository.countByCategory_Name("WIFI"));
        map.put("ELECTRICITY", complaintRepository.countByCategory_Name("ELECTRICITY"));
        map.put("HOSTEL", complaintRepository.countByCategory_Name("HOSTEL"));
        map.put("MAINTENANCE", complaintRepository.countByCategory_Name("MAINTENANCE"));
        map.put("GENERAL", complaintRepository.countByCategory_Name("GENERAL"));

        return map;
    }

    public List<Complaint> getComplaintsByCategory(String category) {
        return complaintRepository.findByCategory_Name(category.toUpperCase());
    }
}