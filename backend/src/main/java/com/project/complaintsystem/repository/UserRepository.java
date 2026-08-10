package com.project.complaintsystem.repository;

import com.project.complaintsystem.model.User;
import com.project.complaintsystem.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);


    List<User> findByDepartment(Department department);
}