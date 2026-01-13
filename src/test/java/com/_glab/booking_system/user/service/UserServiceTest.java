package com._glab.booking_system.user.service;

import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.exception.InvalidRoleException;
import com._glab.booking_system.user.exception.UserAlreadyExistsException;
import com._glab.booking_system.user.exception.UsernameAlreadyExistsException;
import com._glab.booking_system.user.model.Degree;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com._glab.booking_system.user.request.CreateUserRequest;
import com._glab.booking_system.user.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private UserService userService;

    private Role professorRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, roleRepository, passwordSetupTokenService, emailService);

        professorRole = new Role();
        professorRole.setId(1);
        professorRole.setName(RoleName.PROFESSOR);

        adminRole = new Role();
        adminRole.setId(2);
        adminRole.setName(RoleName.ADMIN);
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
}
