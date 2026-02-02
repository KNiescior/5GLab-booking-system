package com._glab.booking_system.user.service;

import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.repository.EmailOtpRepository;
import com._glab.booking_system.auth.repository.PasswordSetupTokenRepository;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.booking.repository.LabManagerRepository;
import com._glab.booking_system.booking.repository.ReservationEditProposalRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.user.exception.InvalidRoleException;
import com._glab.booking_system.user.exception.UserAlreadyExistsException;
import com._glab.booking_system.user.exception.UserNotFoundException;
import com._glab.booking_system.user.exception.UsernameAlreadyExistsException;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com._glab.booking_system.user.request.AdminUpdateUserRequest;
import com._glab.booking_system.user.request.CreateUserRequest;
import com._glab.booking_system.user.request.UpdateProfileRequest;
import com._glab.booking_system.user.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

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
    private final RefreshTokenRepository refreshTokenRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationEditProposalRepository reservationEditProposalRepository;
    private final LabManagerRepository labManagerRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;

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

    /**
     * Update own profile (firstName, lastName, degree, email).
     * Email must be unique if changed.
     *
     * @param user    The authenticated user
     * @param request The update request
     * @return The updated user response
     */
    @Transactional
    public UserResponse updateOwnProfile(User user, UpdateProfileRequest request) {
        log.info("User {} updating own profile", user.getEmail());
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getDegree() != null) {
            user.setDegree(request.getDegree());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Profile update failed: email {} already exists", request.getEmail());
                throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
            }
            user.setEmail(request.getEmail());
        }
        user = userRepository.save(user);
        log.info("User {} profile updated", user.getEmail());
        return UserResponse.fromUser(user);
    }

    /**
     * Update a user by admin (profile and optionally role).
     *
     * @param userId  The user ID to update
     * @param request The update request
     * @return The updated user response
     */
    @Transactional
    public UserResponse updateUserByAdmin(Integer userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        log.info("Admin updating user {} (id={})", user.getEmail(), userId);
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getDegree() != null) {
            user.setDegree(request.getDegree());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRoleName() != null) {
            Role role = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new InvalidRoleException("Invalid role: " + request.getRoleName()));
            user.setRole(role);
        }
        user = userRepository.save(user);
        log.info("User {} updated by admin", user.getEmail());
        return UserResponse.fromUser(user);
    }

    /**
     * Change a user's role (admin-only).
     *
     * @param userId  The user ID
     * @param roleName The new role
     * @return The updated user response
     */
    @Transactional
    public UserResponse changeRole(Integer userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new InvalidRoleException("Invalid role: " + roleName));
        user.setRole(role);
        user = userRepository.save(user);
        log.info("User {} role changed to {} by admin", user.getEmail(), roleName);
        return UserResponse.fromUser(user);
    }

    /**
     * Deactivate a user (soft delete). Sets enabled=false and archivedAt.
     * Revokes all refresh tokens. Does not delete data.
     *
     * @param userId The user ID to deactivate
     * @return The updated user response
     */
    @Transactional
    public UserResponse deactivateUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (Boolean.TRUE.equals(user.getIsAnonymous())) {
            throw new IllegalArgumentException("Cannot deactivate anonymous user");
        }
        log.info("Deactivating user {} (id={})", user.getEmail(), userId);
        user.setEnabled(false);
        user.setArchivedAt(OffsetDateTime.now());
        user = userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user, OffsetDateTime.now());
        log.info("User {} deactivated", user.getEmail());
        return UserResponse.fromUser(user);
    }

    /**
     * Get the system anonymous user (placeholder for deleted accounts).
     * Creates it if not present (e.g. in tests).
     */
    public User getAnonymousUser() {
        return userRepository.findByIsAnonymousTrue()
                .orElseThrow(() -> new IllegalStateException("Anonymous system user not found. Ensure DataInitializer has run."));
    }

    /**
     * Hard delete a user and all related data. Reassigns reservations and edit proposals to anonymous user.
     * Admin-only.
     *
     * @param userId The user ID to delete
     */
    @Transactional
    public void hardDeleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (Boolean.TRUE.equals(user.getIsAnonymous())) {
            throw new IllegalArgumentException("Cannot delete anonymous user");
        }
        User anonymous = getAnonymousUser();
        log.info("Hard deleting user {} (id={})", user.getEmail(), userId);

        reservationRepository.findByUserId(userId).forEach(r -> {
            r.setUser(anonymous);
            reservationRepository.save(r);
        });
        reservationEditProposalRepository.findByEditedBy(user).forEach(ep -> {
            ep.setEditedBy(anonymous);
            reservationEditProposalRepository.save(ep);
        });
        labManagerRepository.findByUser(user).forEach(labManagerRepository::delete);
        refreshTokenRepository.findByUser(user).forEach(refreshTokenRepository::delete);
        emailOtpRepository.findByUser(user).forEach(emailOtpRepository::delete);
        passwordSetupTokenRepository.findByUser(user).forEach(passwordSetupTokenRepository::delete);
        userRepository.delete(user);
        log.info("User {} hard deleted", user.getEmail());
    }
}


