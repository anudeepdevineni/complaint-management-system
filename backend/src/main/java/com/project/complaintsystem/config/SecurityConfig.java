package com.project.complaintsystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                .cors(cors -> {})

                //  Disable CSRF
                .csrf(csrf -> csrf.disable())

                //  Authorization
                .authorizeHttpRequests(auth -> auth

                        //  CRITICAL FIX (this solves your error)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // allow signup/login
                        .requestMatchers("/api/auth/**").permitAll()

                        //  allow all (for now)
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}