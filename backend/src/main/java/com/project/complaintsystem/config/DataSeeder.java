package com.project.complaintsystem.config;

import com.project.complaintsystem.model.*;
import com.project.complaintsystem.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Fills the lookup tables (roles, departments, priorities, statuses, categories)
 * on startup if they are empty, so a fresh database needs no manual SQL.
 * Also creates the admin account from ADMIN_EMAIL / ADMIN_PASSWORD if it doesn't
 * exist yet, since admins can't sign up through the API.
 *
 * Each block only inserts when its rows are missing, so restarting the app never
 * creates duplicates.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PriorityRepository priorityRepository;
    private final StatusRepository statusRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @Value("${admin.name:Admin}")
    private String adminName;

    public DataSeeder(RoleRepository roleRepository,
                      DepartmentRepository departmentRepository,
                      PriorityRepository priorityRepository,
                      StatusRepository statusRepository,
                      CategoryRepository categoryRepository,
                      UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.priorityRepository = priorityRepository;
        this.statusRepository = statusRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        seedRoles();
        seedDepartments();
        seedPriorities();
        seedStatuses();
        seedCategories();
        seedAdmin();
    }

    private void seedAdmin() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            System.out.println("[DataSeeder] ADMIN_EMAIL / ADMIN_PASSWORD not set, no admin created");
            return;
        }
        if (userRepository.existsByEmail(adminEmail)) return;

        User admin = new User();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(new BCryptPasswordEncoder().encode(adminPassword));
        admin.setRole(roleRepository.findByName("ADMIN").orElseThrow());
        userRepository.save(admin);
        System.out.println("[DataSeeder] Created admin " + adminEmail);
    }

    private void seedRoles() {
        if (roleRepository.count() > 0) return;
        for (String name : List.of("STUDENT", "STAFF", "ADMIN")) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
        System.out.println("[DataSeeder] Seeded roles");
    }

    private void seedDepartments() {
        if (departmentRepository.count() > 0) return;
        for (String name : List.of("WIFI", "ELECTRICITY", "HOSTEL", "MAINTENANCE")) {
            Department dept = new Department();
            dept.setName(name);
            departmentRepository.save(dept);
        }
        System.out.println("[DataSeeder] Seeded departments");
    }

    private void seedPriorities() {
        if (priorityRepository.count() > 0) return;
        for (String name : List.of("LOW", "MEDIUM", "HIGH")) {
            Priority priority = new Priority();
            priority.setName(name);
            priorityRepository.save(priority);
        }
        System.out.println("[DataSeeder] Seeded priorities");
    }

    private void seedStatuses() {
        if (statusRepository.count() > 0) return;
        for (String name : List.of("OPEN", "IN_PROGRESS", "RESOLVED", "ESCALATED")) {
            Status status = new Status();
            status.setName(name);
            statusRepository.save(status);
        }
        System.out.println("[DataSeeder] Seeded statuses");
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;

        // The four "real" categories each map to the department that handles them,
        // so complaints auto-assign to staff in that department.
        for (String name : List.of("WIFI", "ELECTRICITY", "HOSTEL", "MAINTENANCE")) {
            Category category = new Category();
            category.setName(name);
            category.setDepartment(departmentRepository.findByName(name).orElse(null));
            categoryRepository.save(category);
        }

        // GENERAL is the fallback category and has no department (stays unassigned).
        Category general = new Category();
        general.setName("GENERAL");
        general.setDepartment(null);
        categoryRepository.save(general);

        System.out.println("[DataSeeder] Seeded categories");
    }
}
