package com.project.complaintsystem.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "priority_id")
    private Priority priority;

    @ManyToOne
    @JoinColumn(name = "assigned_staff_id")
    private User assignedStaff;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    private LocalDateTime createdAt;
    private boolean escalated;

    public Complaint() {
        this.createdAt = LocalDateTime.now();
        this.escalated = false;
    }

    // ===== GETTERS & SETTERS =====
    public Long getId() { return id; }

    public String getTitle() { return title; }

    public String getDescription() { return description; }

    public Status getStatus() { return status; }

    public Category getCategory() { return category; }

    public Priority getPriority() { return priority; }

    public User getAssignedStaff() { return assignedStaff; }

    public User getStudent() { return student; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isEscalated() { return escalated; }

    public void setId(Long id) { this.id = id; }

    public void setTitle(String title) { this.title = title; }

    public void setDescription(String description) { this.description = description; }

    public void setStatus(Status status) { this.status = status; }

    public void setCategory(Category category) { this.category = category; }

    public void setPriority(Priority priority) { this.priority = priority; }

    public void setAssignedStaff(User assignedStaff) { this.assignedStaff = assignedStaff; }

    public void setStudent(User student) { this.student = student; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setEscalated(boolean escalated) { this.escalated = escalated; }
}