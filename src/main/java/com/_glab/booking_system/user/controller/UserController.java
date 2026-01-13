package com._glab.booking_system.user.controller;

import com._glab.booking_system.user.request.CreateUserRequest;
import com._glab.booking_system.user.response.UserResponse;
import com._glab.booking_system.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for user management endpoints.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Register a new user (admin-only).
     * Creates a disabled user and sends an account setup email.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Admin creating new user with email: {}", request.getEmail());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a user by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        UserResponse response = userService.getUserById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Check if a username is available.
     */
    @GetMapping("/check-username")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkUsername(@RequestParam String username) {
        boolean available = userService.isUsernameAvailable(username);
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Check if an email is available.
     */
    @GetMapping("/check-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean available = userService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of("available", available));
    }
}
