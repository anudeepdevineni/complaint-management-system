package com.project.complaintsystem.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Catches exceptions thrown anywhere in the controllers/services and turns them
 * into clean JSON responses with the right HTTP status code, e.g.
 *   { "status": 400, "error": "Wrong password" }
 * The frontend shows the "error" text to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Missing tokens and wrong roles are handled earlier, in SecurityConfig.

    // A request for a file that doesn't exist is a 404, not a server error.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Not found.");
    }

    // Our services throw RuntimeException with a readable message. We pick a
    // status code based on what the message means.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        HttpStatus status = statusForMessage(message);
        return build(status, message);
    }

    // Last-resort catch-all so nothing ever leaks a raw stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }

    // Map a known message to the most fitting status code.
    private HttpStatus statusForMessage(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;              // 404
        }
        if (lower.contains("allowed") || lower.contains("not your")) {
            return HttpStatus.FORBIDDEN;              // 403
        }
        if (lower.contains("already exists")
                || lower.contains("wrong password")
                || lower.contains("invalid status")) {
            return HttpStatus.BAD_REQUEST;            // 400
        }
        // Default: treat unknown errors as a bad request so the message still shows.
        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = Map.of(
                "status", status.value(),
                "error", message
        );
        return ResponseEntity.status(status).body(body);
    }
}
