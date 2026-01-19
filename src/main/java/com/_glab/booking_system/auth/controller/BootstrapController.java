package com._glab.booking_system.auth.controller;

import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Bootstrap endpoint for initial admin setup on fresh deployment.
 * Only works when no admin user exists in the database.
 * 
 * TODO (PRODUCTION): Consider removing or securing this endpoint after initial setup.
 */
@RestController
@RequestMapping("/api/v1/bootstrap")
@RequiredArgsConstructor
@Slf4j
public class BootstrapController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordSetupTokenService passwordSetupTokenService;
    private final EmailService emailService;

    @Data
    public static class BootstrapAdminRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String firstName;
        @NotBlank
        private String lastName;
    }

    /**
     * Create the first admin user. Only works if no admin exists.
     * Sends password setup email to the provided address.
     */
    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> createFirstAdmin(@Valid @RequestBody BootstrapAdminRequest request) {
        // Check if any admin already exists
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() != null && u.getRole().getName() == RoleName.ADMIN);
        
        if (adminExists) {
            log.warn("Bootstrap admin attempt rejected - admin already exists");
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "ADMIN_EXISTS",
                    "message", "An admin user already exists. Bootstrap is disabled."
            ));
        }

        // Ensure roles exist
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(RoleName.ADMIN);
                    return roleRepository.save(role);
                });

        // Create admin user (disabled, no password)
        User admin = new User();
        admin.setEmail(request.getEmail());
        admin.setUsername("admin");
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setRole(adminRole);
        admin.setEnabled(false); // Will be enabled after password setup
        userRepository.save(admin);

        // Generate password setup token and send email
        String token = passwordSetupTokenService.createToken(admin, TokenPurpose.ACCOUNT_SETUP);
        emailService.sendAccountSetupEmail(admin, token);

        log.info("Bootstrap admin created: {} - password setup email sent", request.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Admin account created. Password setup email sent to " + request.getEmail(),
                "note", "The admin will need to set their password and configure MFA on their own device."
        ));
    }

    /**
     * Check if bootstrap is available (no admin exists).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBootstrapStatus() {
        boolean adminExists = userRepository.findAll().stream()
                .anyMatch(u -> u.getRole() != null && u.getRole().getName() == RoleName.ADMIN);
        
        return ResponseEntity.ok(Map.of(
                "bootstrapAvailable", !adminExists,
                "message", adminExists 
                        ? "Admin exists - bootstrap disabled" 
                        : "No admin found - bootstrap available"
        ));
    }
}
