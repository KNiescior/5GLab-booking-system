package com._glab.booking_system.user.service;

import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.exception.InvalidRoleException;
import com._glab.booking_system.user.exception.UserAlreadyExistsException;
import com._glab.booking_system.user.exception.UsernameAlreadyExistsException;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com._glab.booking_system.user.request.CreateUserRequest;
import com._glab.booking_system.user.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordSetupTokenService passwordSetupTokenService;
    private final EmailService emailService;

    /**
     * Register a new user (admin-only operation).
     * Creates a disabled user and sends an account setup email.
     *
     * @param request The user creation request
     * @return The created user response
     */
    @Transactional
    public UserResponse registerUser(CreateUserRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Validate email is unique
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("Registration failed: email {} already exists", request.getEmail());
            throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
        }

        // Validate username is unique
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            log.warn("Registration failed: username {} already exists", request.getUsername());
            throw new UsernameAlreadyExistsException("A user with username " + request.getUsername() + " already exists");
        }

        // Look up role
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> {
                    log.warn("Registration failed: invalid role {}", request.getRoleName());
                    return new InvalidRoleException("Invalid role: " + request.getRoleName());
                });

        // Create user entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setDegree(request.getDegree());
        user.setRole(role);
        user.setEnabled(false); // Disabled until password is set
        user.setPassword(null); // No password until setup

        // Save user
        user = userRepository.save(user);
        log.info("User {} created with ID {}", user.getEmail(), user.getId());

        // Generate password setup token
        String token = passwordSetupTokenService.createToken(user, TokenPurpose.ACCOUNT_SETUP);
        log.debug("Password setup token generated for user {}", user.getEmail());

        // Send account setup email
        emailService.sendAccountSetupEmail(user, token);

        return UserResponse.fromUser(user);
    }

    /**
     * Get a user by ID.
     *
     * @param id The user ID
     * @return The user response, or null if not found
     */
    public UserResponse getUserById(Integer id) {
        return userRepository.findById(id)
                .map(UserResponse::fromUser)
                .orElse(null);
    }

    /**
     * Check if a username is available.
     *
     * @param username The username to check
     * @return true if available, false if taken
     */
    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUsername(username).isEmpty();
    }

    /**
     * Check if an email is available.
     *
     * @param email The email to check
     * @return true if available, false if taken
     */
    public boolean isEmailAvailable(String email) {
        return userRepository.findByEmail(email).isEmpty();
    }
}
