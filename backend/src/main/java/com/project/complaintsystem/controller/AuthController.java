package com.project.complaintsystem.controller;

import com.project.complaintsystem.dto.LoginRequest;
import com.project.complaintsystem.dto.SignupRequest;
import com.project.complaintsystem.dto.UserResponse;
import com.project.complaintsystem.model.User;
import com.project.complaintsystem.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/signup")
    public UserResponse signup(@RequestBody SignupRequest req) {
        User user = authService.signup(
                req.getName(),
                req.getEmail(),
                req.getPassword(),
                req.getRole(),
                req.getDepartment()
        );
        return UserResponse.from(user);
    }

    // Returns the signed JWT as a plain string.
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest req){
        return authService.login(req.getEmail(), req.getPassword());
    }
}