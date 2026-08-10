package com.project.complaintsystem.repository;

import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.model.ComplaintHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {

    // Full change log for one complaint, oldest change first.
    List<ComplaintHistory> findByComplaintOrderByChangedAtAsc(Complaint complaint);
}
