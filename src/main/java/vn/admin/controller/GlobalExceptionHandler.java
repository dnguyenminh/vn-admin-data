package vn.admin.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Test-only exception handler that surfaces minimal trace information to aid
 * debugging during integration tests. This is intentionally limited to the
 * {@code test} profile to avoid leaking stack traces in production.
 */
@ControllerAdvice
@Profile("test")
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String trace = sw.toString();
        log.error("Unhandled exception in controller: {}\n{}", ex.getMessage(), trace);
        // Return a minimal error body so tests can see the message (trim to 1024 chars)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                java.util.Map.of("error", ex.getMessage(), "trace", trace.substring(0, Math.min(1024, trace.length()))));
    }
}
