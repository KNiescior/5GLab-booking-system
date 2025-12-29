package com._glab.booking_system.auth.controller;

import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.auth.exception.AccountDisabledException;
import com._glab.booking_system.auth.exception.AccountLockedException;
import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.auth.model.RefreshToken;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.request.LoginRequest;
import com._glab.booking_system.auth.response.LoginResponse;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.auth.service.MfaService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordSetupTokenService passwordSetupTokenService;

    @Mock
    private MfaService mfaService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @InjectMocks
    private LoginController loginController;

    private User testUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1);
        testRole.setName(RoleName.PROFESSOR);

        testUser = new User();
        testUser.setId(1);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setEnabled(true);
        testUser.setFailedLoginCount(0);
        testUser.setRole(testRole);

        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Nested
    @DisplayName("Successful Login Tests")
    class SuccessfulLoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() {
            // Given
            LoginRequest request = new LoginRequest("test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(mfaService.needsMfaSetup(testUser)).thenReturn(false);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn(
                    new JwtService.RefreshTokenResult("refresh-token", "jti-123", new Date())
            );

            // When
            ResponseEntity<?> response = loginController.loginUser(request, httpRequest, httpResponse);

            // Then
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isInstanceOf(LoginResponse.class);
            LoginResponse loginResponse = (LoginResponse) response.getBody();
            assertThat(loginResponse.getAccessToken()).isEqualTo("access-token");
            assertThat(loginResponse.getUser().getEmail()).isEqualTo("test@example.com");

            // Verify lockout counters reset
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(0);
            assertThat(userCaptor.getValue().getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should reset lockout counters on successful login after failed attempts")
        void shouldResetLockoutOnSuccess() {
            // Given
            testUser.setFailedLoginCount(2);
            LoginRequest request = new LoginRequest("test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(mfaService.needsMfaSetup(testUser)).thenReturn(false);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn(
                    new JwtService.RefreshTokenResult("refresh-token", "jti-123", new Date())
            );

            // When
            loginController.loginUser(request, httpRequest, httpResponse);

            // Then
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Failed Login Tests")
    class FailedLoginTests {

        @Test
        @DisplayName("Should throw exception for unknown email")
        void shouldThrowForUnknownEmail() {
            LoginRequest request = new LoginRequest("unknown@example.com", "password");
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for wrong password")
        void shouldThrowForWrongPassword() {
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw exception for disabled account")
        void shouldThrowForDisabledAccount() {
            testUser.setEnabled(false);
            LoginRequest request = new LoginRequest("test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AccountDisabledException.class)
                    .hasMessage("Account is disabled");
        }

        @Test
        @DisplayName("Should throw exception for empty credentials")
        void shouldThrowForEmptyCredentials() {
            LoginRequest request = new LoginRequest("", "");

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class)
                    .hasMessage("Invalid credentials");
        }
    }

    @Nested
    @DisplayName("Lockout Policy Tests")
    class LockoutPolicyTests {

        @Test
        @DisplayName("Should increment failed login count on wrong password")
        void shouldIncrementFailedCount() {
            testUser.setFailedLoginCount(0);
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(1);
            assertThat(userCaptor.getValue().getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should lock account for 10 minutes after 3 failed attempts")
        void shouldLockFor10MinutesAfter3Failures() {
            testUser.setFailedLoginCount(2);
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(3);
            assertThat(userCaptor.getValue().getLockedUntil()).isNotNull();
            // Should be approximately 10 minutes from now
            assertThat(userCaptor.getValue().getLockedUntil())
                    .isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(9))
                    .isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(11));
        }

        @Test
        @DisplayName("Should lock account for 30 minutes after 6 failed attempts")
        void shouldLockFor30MinutesAfter6Failures() {
            testUser.setFailedLoginCount(5);
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(6);
            assertThat(userCaptor.getValue().getLockedUntil()).isNotNull();
            // Should be approximately 30 minutes from now
            assertThat(userCaptor.getValue().getLockedUntil())
                    .isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(29))
                    .isBefore(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(31));
        }

        @Test
        @DisplayName("Should reject login when account is locked")
        void shouldRejectWhenLocked() {
            testUser.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5));
            LoginRequest request = new LoginRequest("test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessageContaining("Account is locked until");
        }

        @Test
        @DisplayName("Should reset failed count when lockout expires")
        void shouldResetCountWhenLockoutExpires() {
            // Lockout expired 1 minute ago
            testUser.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            testUser.setFailedLoginCount(6);
            LoginRequest request = new LoginRequest("test@example.com", "password");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
            when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn(
                    new JwtService.RefreshTokenResult("refresh-token", "jti-123", new Date())
            );

            loginController.loginUser(request, httpRequest, httpResponse);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(0);
            assertThat(userCaptor.getValue().getLockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should start fresh lockout tier after lockout expires and new failure")
        void shouldStartFreshTierAfterExpiredLockout() {
            // Lockout expired
            testUser.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            testUser.setFailedLoginCount(6);
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

            assertThatThrownBy(() -> loginController.loginUser(request, httpRequest, httpResponse))
                    .isInstanceOf(AuthenticationFailedException.class);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            // Should be 1 (reset to 0, then incremented to 1)
            assertThat(userCaptor.getValue().getFailedLoginCount()).isEqualTo(1);
            // No lockout yet (only 1 failure)
            assertThat(userCaptor.getValue().getLockedUntil()).isNull();
        }
    }
}

