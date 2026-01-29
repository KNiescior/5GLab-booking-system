package com._glab.booking_system.user.controller;

import com._glab.booking_system.user.request.AdminUpdateUserRequest;
import com._glab.booking_system.user.request.ChangeRoleRequest;
import com._glab.booking_system.user.response.UserResponse;
import com._glab.booking_system.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only controller for user management (edit profiles, change roles).
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    /**
     * Update any user's profile (and optionally role).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable("id") Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        log.info("Admin updating user id={}", userId);
        UserResponse response = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Change a user's role.
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable("id") Integer userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        log.info("Admin changing role for user id={} to {}", userId, request.getRoleName());
        UserResponse response = userService.changeRole(userId, request.getRoleName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate a user (soft delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable("id") Integer userId) {
        log.info("Admin deactivating user id={}", userId);
        UserResponse response = userService.deactivateUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Hard delete a user (GDPR). Reassigns reservations to anonymous user and deletes all related data.
     */
    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteUser(@PathVariable("id") Integer userId) {
        log.info("Admin hard deleting user id={}", userId);
        userService.hardDeleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}


