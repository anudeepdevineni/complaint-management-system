package com.project.complaintsystem.dto;

import com.project.complaintsystem.model.User;

/**
 * The shape of a user as sent back after signup — flat, and with no password.
 */
public class UserResponse {

    public Long id;
    public String name;
    public String email;
    public String role;
    public String department;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.name = u.getName();
        r.email = u.getEmail();
        r.role = u.getRole() != null ? u.getRole().getName() : null;
        r.department = u.getDepartment() != null ? u.getDepartment().getName() : null;
        return r;
    }
}
