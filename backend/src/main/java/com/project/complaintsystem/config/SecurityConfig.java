package com.project.complaintsystem.config;

import com.project.complaintsystem.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Route-level authorization. Every /api/complaints endpoint is tied to a role here,
 * and JwtAuthFilter supplies the role from the token.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})   // uses the CorsConfigurationSource bean from CorsConfig
                .csrf(csrf -> csrf.disable())   // stateless JWT API, no cookies
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**", "/error").permitAll()

                        // Student
                        .requestMatchers(HttpMethod.POST, "/api/complaints").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/complaints/my").hasRole("STUDENT")

                        // Staff
                        .requestMatchers(HttpMethod.GET, "/api/complaints/assigned").hasRole("STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/complaints/status").hasRole("STAFF")

                        // Admin: list, filter, summary, history, analytics
                        .requestMatchers(HttpMethod.GET, "/api/complaints", "/api/complaints/**").hasRole("ADMIN")

                        // Any other /api path is closed
                        .requestMatchers("/api/**").denyAll()
                        // Everything else is the React app and its static files
                        .anyRequest().permitAll()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, 401, "Please log in to continue."))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, 403, "You don't have access to this.")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Same { status, error } shape as GlobalExceptionHandler, so the frontend can show it.
    private static void writeError(HttpServletResponse res, int status, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + message + "\"}");
    }
}
