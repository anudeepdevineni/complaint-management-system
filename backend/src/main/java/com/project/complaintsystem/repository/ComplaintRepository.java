package com.project.complaintsystem.repository;

import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudent(User student);
    List<Complaint> findByAssignedStaff(User staff);
    long countByStatus_Name(String name);
    long countByCategory_Name(String name);
    List<Complaint> findByCategory_Name(String name);

    long countByAssignedStaffAndStatus_Name(User staff, String statusName);

    // Counts a staff member's ACTIVE complaints — everything except the given status.
    // Called with "RESOLVED" so it returns OPEN + IN_PROGRESS + ESCALATED, i.e. the
    // current workload used to pick the least-loaded worker on assignment.
    long countByAssignedStaffAndStatus_NameNot(User staff, String statusName);
}