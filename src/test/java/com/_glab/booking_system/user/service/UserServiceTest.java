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
import com._glab.booking_system.user.model.Degree;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com._glab.booking_system.user.request.AdminUpdateUserRequest;
import com._glab.booking_system.user.request.CreateUserRequest;
import com._glab.booking_system.user.request.UpdateProfileRequest;
import com._glab.booking_system.user.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordSetupTokenService passwordSetupTokenService;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationEditProposalRepository reservationEditProposalRepository;

    @Mock
    private LabManagerRepository labManagerRepository;

    @Mock
    private EmailOtpRepository emailOtpRepository;

    @Mock
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    private UserService userService;

    private Role professorRole;
    private Role adminRole;
    private Role labManagerRole;
    private User testUser;
    private User anonymousUser;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                roleRepository,
                passwordSetupTokenService,
                emailService,
                refreshTokenRepository,
                reservationRepository,
                reservationEditProposalRepository,
                labManagerRepository,
                emailOtpRepository,
                passwordSetupTokenRepository
        );

        professorRole = new Role();
        professorRole.setId(1);
        professorRole.setName(RoleName.PROFESSOR);

        adminRole = new Role();
        adminRole.setId(2);
        adminRole.setName(RoleName.ADMIN);

        labManagerRole = new Role();
        labManagerRole.setId(3);
        labManagerRole.setName(RoleName.LAB_MANAGER);

        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setDegree(Degree.DR);
        testUser.setRole(professorRole);
        testUser.setEnabled(true);
        testUser.setIsAnonymous(false);

        anonymousUser = new User();
        anonymousUser.setId(999);
        anonymousUser.setEmail("anonymous@system.local");
        anonymousUser.setUsername("__anonymous__");
        anonymousUser.setFirstName("Deleted");
        anonymousUser.setLastName("User");
        anonymousUser.setRole(professorRole);
        anonymousUser.setEnabled(false);
        anonymousUser.setIsAnonymous(true);
    }

    @Nested
    @DisplayName("Register User Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("test@example.com")
                    .username("tesuse")
                    .firstName("Test")
                    .lastName("User")
                    .degree(Degree.DR)
                    .roleName(RoleName.PROFESSOR)
                    .build();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.PROFESSOR)).thenReturn(Optional.of(professorRole));
            when(passwordSetupTokenService.createToken(any(User.class), eq(TokenPurpose.ACCOUNT_SETUP)))
                    .thenReturn("test-token");

            User savedUser = new User();
            savedUser.setId(1);
            savedUser.setEmail(request.getEmail());
            savedUser.setUsername(request.getUsername());
            savedUser.setFirstName(request.getFirstName());
            savedUser.setLastName(request.getLastName());
            savedUser.setDegree(request.getDegree());
            savedUser.setRole(professorRole);
            savedUser.setEnabled(false);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = userService.registerUser(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1);
            assertThat(response.getEmail()).isEqualTo(request.getEmail());
            assertThat(response.getUsername()).isEqualTo(request.getUsername());
            assertThat(response.getFirstName()).isEqualTo(request.getFirstName());
            assertThat(response.getLastName()).isEqualTo(request.getLastName());
            assertThat(response.getDegree()).isEqualTo(Degree.DR);
            assertThat(response.getRole()).isEqualTo(RoleName.PROFESSOR);
            assertThat(response.getEnabled()).isFalse();

            // Verify user was saved with correct values
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertThat(capturedUser.getEnabled()).isFalse();
            assertThat(capturedUser.getPassword()).isNull();

            // Verify token was created and email was sent
            verify(passwordSetupTokenService).createToken(any(User.class), eq(TokenPurpose.ACCOUNT_SETUP));
            verify(emailService).sendAccountSetupEmail(any(User.class), eq("test-token"));
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("existing@example.com")
                    .username("newuser")
                    .firstName("New")
                    .lastName("User")
                    .roleName(RoleName.PROFESSOR)
                    .build();

            User existingUser = new User();
            existingUser.setEmail(request.getEmail());
            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("existing@example.com");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendAccountSetupEmail(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .username("existinguser")
                    .firstName("New")
                    .lastName("User")
                    .roleName(RoleName.PROFESSOR)
                    .build();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

            User existingUser = new User();
            existingUser.setUsername(request.getUsername());
            when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(existingUser));

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(UsernameAlreadyExistsException.class)
                    .hasMessageContaining("existinguser");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendAccountSetupEmail(any(), any());
        }

        @Test
        @DisplayName("Should throw exception when role is invalid")
        void shouldThrowExceptionWhenRoleInvalid() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("new@example.com")
                    .username("newuser")
                    .firstName("New")
                    .lastName("User")
                    .roleName(RoleName.ADMIN)
                    .build();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.registerUser(request))
                    .isInstanceOf(InvalidRoleException.class)
                    .hasMessageContaining("ADMIN");

            verify(userRepository, never()).save(any(User.class));
            verify(emailService, never()).sendAccountSetupEmail(any(), any());
        }

        @Test
        @DisplayName("Should register user without degree")
        void shouldRegisterUserWithoutDegree() {
            // Given
            CreateUserRequest request = CreateUserRequest.builder()
                    .email("nodegree@example.com")
                    .username("nodegr")
                    .firstName("No")
                    .lastName("Degree")
                    .degree(null)
                    .roleName(RoleName.PROFESSOR)
                    .build();

            when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
            when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.PROFESSOR)).thenReturn(Optional.of(professorRole));
            when(passwordSetupTokenService.createToken(any(User.class), eq(TokenPurpose.ACCOUNT_SETUP)))
                    .thenReturn("test-token");

            User savedUser = new User();
            savedUser.setId(1);
            savedUser.setEmail(request.getEmail());
            savedUser.setUsername(request.getUsername());
            savedUser.setFirstName(request.getFirstName());
            savedUser.setLastName(request.getLastName());
            savedUser.setDegree(null);
            savedUser.setRole(professorRole);
            savedUser.setEnabled(false);

            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When
            UserResponse response = userService.registerUser(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDegree()).isNull();
        }
    }

    @Nested
    @DisplayName("Availability Check Tests")
    class AvailabilityCheckTests {

        @Test
        @DisplayName("Should return true when username is available")
        void shouldReturnTrueWhenUsernameAvailable() {
            when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

            assertThat(userService.isUsernameAvailable("newuser")).isTrue();
        }

        @Test
        @DisplayName("Should return false when username is taken")
        void shouldReturnFalseWhenUsernameTaken() {
            User existingUser = new User();
            existingUser.setUsername("takenuser");
            when(userRepository.findByUsername("takenuser")).thenReturn(Optional.of(existingUser));

            assertThat(userService.isUsernameAvailable("takenuser")).isFalse();
        }

        @Test
        @DisplayName("Should return true when email is available")
        void shouldReturnTrueWhenEmailAvailable() {
            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

            assertThat(userService.isEmailAvailable("new@example.com")).isTrue();
        }

        @Test
        @DisplayName("Should return false when email is taken")
        void shouldReturnFalseWhenEmailTaken() {
            User existingUser = new User();
            existingUser.setEmail("taken@example.com");
            when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(existingUser));

            assertThat(userService.isEmailAvailable("taken@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("Get User Tests")
    class GetUserTests {

        @Test
        @DisplayName("Should return user when found")
        void shouldReturnUserWhenFound() {
            User user = new User();
            user.setId(1);
            user.setEmail("test@example.com");
            user.setUsername("testuser");
            user.setFirstName("Test");
            user.setLastName("User");
            user.setRole(professorRole);
            user.setEnabled(true);

            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            UserResponse response = userService.getUserById(1);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1);
            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should return null when user not found")
        void shouldReturnNullWhenUserNotFound() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            UserResponse response = userService.getUserById(999);

            assertThat(response).isNull();
        }
    }

    @Nested
    @DisplayName("Update Own Profile Tests")
    class UpdateOwnProfileTests {

        @Test
        @DisplayName("Should update all profile fields successfully")
        void shouldUpdateAllProfileFieldsSuccessfully() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("Updated")
                    .lastName("Name")
                    .degree(Degree.PROF)
                    .email("newemail@example.com")
                    .build();

            when(userRepository.findByEmail("newemail@example.com")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateOwnProfile(testUser, request);

            assertThat(response.getFirstName()).isEqualTo("Updated");
            assertThat(response.getLastName()).isEqualTo("Name");
            assertThat(response.getDegree()).isEqualTo(Degree.PROF);
            assertThat(response.getEmail()).isEqualTo("newemail@example.com");
        }

        @Test
        @DisplayName("Should update partial profile fields")
        void shouldUpdatePartialProfileFields() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .firstName("OnlyFirst")
                    .build();

            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateOwnProfile(testUser, request);

            assertThat(response.getFirstName()).isEqualTo("OnlyFirst");
            assertThat(response.getLastName()).isEqualTo("User"); // unchanged
            assertThat(response.getDegree()).isEqualTo(Degree.DR); // unchanged
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .email("taken@example.com")
                    .build();

            User otherUser = new User();
            otherUser.setEmail("taken@example.com");
            when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.updateOwnProfile(testUser, request))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("taken@example.com");
        }

        @Test
        @DisplayName("Should allow keeping same email")
        void shouldAllowKeepingSameEmail() {
            UpdateProfileRequest request = UpdateProfileRequest.builder()
                    .email("test@example.com") // Same as testUser's email
                    .build();

            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateOwnProfile(testUser, request);

            assertThat(response.getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Admin Update User Tests")
    class AdminUpdateUserTests {

        @Test
        @DisplayName("Should update user profile and role by admin")
        void shouldUpdateUserProfileAndRoleByAdmin() {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .firstName("AdminUpdated")
                    .lastName("ByAdmin")
                    .degree(Degree.MGR)
                    .email("adminupdated@example.com")
                    .roleName(RoleName.LAB_MANAGER)
                    .build();

            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("adminupdated@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName(RoleName.LAB_MANAGER)).thenReturn(Optional.of(labManagerRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.updateUserByAdmin(1, request);

            assertThat(response.getFirstName()).isEqualTo("AdminUpdated");
            assertThat(response.getLastName()).isEqualTo("ByAdmin");
            assertThat(response.getDegree()).isEqualTo(Degree.MGR);
            assertThat(response.getEmail()).isEqualTo("adminupdated@example.com");
            assertThat(response.getRole()).isEqualTo(RoleName.LAB_MANAGER);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowUserNotFoundException() {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .firstName("Updated")
                    .build();

            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserByAdmin(999, request))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .email("taken@example.com")
                    .build();

            User otherUser = new User();
            otherUser.setEmail("taken@example.com");

            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.updateUserByAdmin(1, request))
                    .isInstanceOf(UserAlreadyExistsException.class);
        }

        @Test
        @DisplayName("Should throw exception when role is invalid")
        void shouldThrowExceptionWhenRoleInvalid() {
            AdminUpdateUserRequest request = AdminUpdateUserRequest.builder()
                    .roleName(RoleName.ADMIN)
                    .build();

            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserByAdmin(1, request))
                    .isInstanceOf(InvalidRoleException.class);
        }
    }

    @Nested
    @DisplayName("Change Role Tests")
    class ChangeRoleTests {

        @Test
        @DisplayName("Should change user role successfully")
        void shouldChangeUserRoleSuccessfully() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.LAB_MANAGER)).thenReturn(Optional.of(labManagerRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.changeRole(1, RoleName.LAB_MANAGER);

            assertThat(response.getRole()).isEqualTo(RoleName.LAB_MANAGER);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowUserNotFoundException() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeRole(999, RoleName.ADMIN))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw InvalidRoleException when role not found")
        void shouldThrowInvalidRoleException() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.changeRole(1, RoleName.ADMIN))
                    .isInstanceOf(InvalidRoleException.class);
        }
    }

    @Nested
    @DisplayName("Deactivate User Tests")
    class DeactivateUserTests {

        @Test
        @DisplayName("Should deactivate user successfully")
        void shouldDeactivateUserSuccessfully() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserResponse response = userService.deactivateUser(1);

            assertThat(response.getEnabled()).isFalse();
            assertThat(testUser.getArchivedAt()).isNotNull();
            verify(refreshTokenRepository).revokeAllForUser(eq(testUser), any(OffsetDateTime.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowUserNotFoundException() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deactivateUser(999))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when trying to deactivate anonymous user")
        void shouldThrowExceptionWhenDeactivatingAnonymousUser() {
            when(userRepository.findById(999)).thenReturn(Optional.of(anonymousUser));

            assertThatThrownBy(() -> userService.deactivateUser(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anonymous");
        }
    }

    @Nested
    @DisplayName("Hard Delete User Tests")
    class HardDeleteUserTests {

        @Test
        @DisplayName("Should hard delete user and reassign reservations to anonymous")
        void shouldHardDeleteUserSuccessfully() {
            when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
            when(userRepository.findByIsAnonymousTrue()).thenReturn(Optional.of(anonymousUser));
            when(reservationRepository.findByUserId(1)).thenReturn(List.of());
            when(reservationEditProposalRepository.findByEditedBy(testUser)).thenReturn(List.of());
            when(labManagerRepository.findByUser(testUser)).thenReturn(List.of());
            when(refreshTokenRepository.findByUser(testUser)).thenReturn(List.of());
            when(emailOtpRepository.findByUser(testUser)).thenReturn(List.of());
            when(passwordSetupTokenRepository.findByUser(testUser)).thenReturn(List.of());

            userService.hardDeleteUser(1);

            verify(userRepository).delete(testUser);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowUserNotFoundException() {
            when(userRepository.findById(999)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.hardDeleteUser(999))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when trying to delete anonymous user")
        void shouldThrowExceptionWhenDeletingAnonymousUser() {
            when(userRepository.findById(999)).thenReturn(Optional.of(anonymousUser));

            assertThatThrownBy(() -> userService.hardDeleteUser(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("anonymous");
        }
    }

    @Nested
    @DisplayName("Get Anonymous User Tests")
    class GetAnonymousUserTests {

        @Test
        @DisplayName("Should return anonymous user when it exists")
        void shouldReturnAnonymousUserWhenExists() {
            when(userRepository.findByIsAnonymousTrue()).thenReturn(Optional.of(anonymousUser));

            User result = userService.getAnonymousUser();

            assertThat(result).isEqualTo(anonymousUser);
            assertThat(result.getIsAnonymous()).isTrue();
        }

        @Test
        @DisplayName("Should throw exception when anonymous user not found")
        void shouldThrowExceptionWhenAnonymousUserNotFound() {
            when(userRepository.findByIsAnonymousTrue()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getAnonymousUser())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Anonymous system user not found");
        }
    }
}
