package com.project.complaintsystem.service;

import com.project.complaintsystem.model.*;
import com.project.complaintsystem.repository.*;
import com.project.complaintsystem.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       DepartmentRepository departmentRepository,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.jwtUtil = jwtUtil;
    }

    // ================= SIGNUP =================
    public User signup(String name, String email, String password, String roleName, String deptName) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // Only students and staff can sign up. The admin account is created at
        // startup from ADMIN_EMAIL / ADMIN_PASSWORD (see DataSeeder).
        String requested = roleName == null ? "STUDENT" : roleName.toUpperCase();
        if (!requested.equals("STUDENT") && !requested.equals("STAFF")) {
            throw new RuntimeException("Signing up as " + requested.toLowerCase() + " is not allowed");
        }

        Role role = roleRepository.findByName(requested)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Department dept = null;

        if (role.getName().equals("STAFF")) {
            if (deptName == null || deptName.isBlank()) {
                throw new RuntimeException("Staff must choose a department");
            }
            dept = departmentRepository.findByName(deptName.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        user.setDepartment(dept);

        return userRepository.save(user);
    }

    // ================= LOGIN =================
    public String login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Wrong password");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole().getName());
    }
}