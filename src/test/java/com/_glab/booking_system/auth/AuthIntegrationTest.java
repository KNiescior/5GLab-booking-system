package com._glab.booking_system.auth;

import com._glab.booking_system.auth.config.TestJwtConfig;
import com._glab.booking_system.auth.config.TestMailConfig;
import com._glab.booking_system.auth.model.PasswordSetupToken;
import com._glab.booking_system.auth.model.RefreshToken;
import com._glab.booking_system.auth.model.TokenPurpose;
import com._glab.booking_system.auth.repository.EmailOtpRepository;
import com._glab.booking_system.auth.repository.PasswordSetupTokenRepository;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.request.LoginRequest;
import com._glab.booking_system.auth.request.MfaVerifyRequest;
import com._glab.booking_system.auth.request.SetupPasswordRequest;
import com._glab.booking_system.auth.model.MfaCodeType;
import com._glab.booking_system.auth.service.MfaService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.model.Role;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.RoleRepository;
import com._glab.booking_system.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Import({TestJwtConfig.class, TestMailConfig.class})
class AuthIntegrationTest {

    // Testcontainers works on Linux CI, but has issues with Docker Desktop + WSL2 on Windows
    // Local Windows: run unit tests only, or use CI for integration tests

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @Autowired
    private PasswordSetupTokenService passwordSetupTokenService;

    @Autowired
    private MfaService mfaService;

    @Autowired
    private EmailOtpRepository emailOtpRepository;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Clean up
        emailOtpRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        passwordSetupTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // Create roles
        userRole = new Role();
        userRole.setName(RoleName.PROFESSOR);
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminRole = roleRepository.save(adminRole);

        // Create test user (Professor - MFA optional)
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);
        testUser.setFailedLoginCount(0);
        testUser.setRole(userRole);
        testUser = userRepository.save(testUser);
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("test@example.com"))
                    .andExpect(jsonPath("$.user.role").value("PROFESSOR"))
                    .andExpect(cookie().exists("refreshToken"))
                    .andExpect(cookie().httpOnly("refreshToken", true));
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        void shouldReturn401ForInvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Should return 401 for unknown email")
        void shouldReturn401ForUnknownEmail() throws Exception {
            LoginRequest request = new LoginRequest("unknown@example.com", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Should return 403 for disabled account")
        void shouldReturn403ForDisabledAccount() throws Exception {
            testUser.setEnabled(false);
            userRepository.save(testUser);

            LoginRequest request = new LoginRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("AUTH_ACCOUNT_DISABLED"));
        }

        @Test
        @DisplayName("Should lock account after 3 failed attempts")
        void shouldLockAccountAfter3Failures() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

            // 3 failed attempts
            for (int i = 0; i < 3; i++) {
                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isUnauthorized());
            }

            // Verify account is locked
            User updated = userRepository.findByEmail("test@example.com").orElseThrow();
            assertThat(updated.getLockedUntil()).isNotNull();
            assertThat(updated.getLockedUntil()).isAfter(OffsetDateTime.now());

            // Next attempt should fail with locked message
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("AUTH_ACCOUNT_LOCKED"));
        }
    }

    @Nested
    @DisplayName("Refresh Endpoint Tests")
    class RefreshEndpointTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // First login to get refresh token
            LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
            assertThat(refreshCookie).isNotNull();

            // Refresh the token
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(refreshCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(cookie().exists("refreshToken"));
        }

        @Test
        @DisplayName("Should return 401 for missing refresh token")
        void shouldReturn401ForMissingRefreshToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_INVALID_REFRESH_TOKEN"));
        }

        @Test
        @DisplayName("Should detect token reuse")
        void shouldDetectTokenReuse() throws Exception {
            // Login to get refresh token
            LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie originalRefreshCookie = loginResult.getResponse().getCookie("refreshToken");

            // First refresh - should succeed
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(originalRefreshCookie))
                    .andExpect(status().isOk());

            // Second refresh with original token - should detect reuse
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(originalRefreshCookie))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_REFRESH_TOKEN_REUSE_DETECTED"));
        }
    }

    @Nested
    @DisplayName("Logout Endpoint Tests")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // Login first
            LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");
            MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

            // Logout
            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(refreshCookie))
                    .andExpect(status().isNoContent())
                    .andExpect(cookie().maxAge("refreshToken", 0));
        }

        @Test
        @DisplayName("Should handle logout without cookie")
        void shouldHandleLogoutWithoutCookie() throws Exception {
            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Password Setup Endpoint Tests")
    class PasswordSetupEndpointTests {

        @Test
        @DisplayName("Should setup password and auto-login")
        void shouldSetupPasswordAndAutoLogin() throws Exception {
            // Create user without password (disabled)
            User newUser = new User();
            newUser.setEmail("newuser@example.com");
            newUser.setUsername("newuser");
            newUser.setEnabled(false);
            newUser.setRole(userRole);
            newUser = userRepository.save(newUser);

            // Create password setup token
            String rawToken = passwordSetupTokenService.createToken(newUser, TokenPurpose.ACCOUNT_SETUP);

            SetupPasswordRequest request = new SetupPasswordRequest(rawToken, "newPassword123");

            mockMvc.perform(post("/api/v1/auth/setup-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                    .andExpect(cookie().exists("refreshToken"));

            // Verify user is now enabled with password
            User updated = userRepository.findByEmail("newuser@example.com").orElseThrow();
            assertThat(updated.getEnabled()).isTrue();
            assertThat(updated.getPassword()).isNotNull();
            assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();
        }

        @Test
        @DisplayName("Should return 400 for invalid token")
        void shouldReturn400ForInvalidToken() throws Exception {
            SetupPasswordRequest request = new SetupPasswordRequest("invalid-token", "newPassword123");

            mockMvc.perform(post("/api/v1/auth/setup-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("AUTH_PASSWORD_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Should return 400 for expired token")
        void shouldReturn400ForExpiredToken() throws Exception {
            // Create user
            User newUser = new User();
            newUser.setEmail("expired@example.com");
            newUser.setUsername("expireduser");
            newUser.setEnabled(false);
            newUser.setRole(userRole);
            final User savedUser = userRepository.save(newUser);

            // Create token and manually expire it
            String rawToken = passwordSetupTokenService.createToken(savedUser, TokenPurpose.ACCOUNT_SETUP);
            
            // Find and expire the token
            PasswordSetupToken token = passwordSetupTokenRepository.findAll().stream()
                    .filter(t -> t.getUser().getId().equals(savedUser.getId()))
                    .findFirst()
                    .orElseThrow();
            token.setExpiresAt(OffsetDateTime.now().minusHours(1));
            passwordSetupTokenRepository.save(token);

            SetupPasswordRequest request = new SetupPasswordRequest(rawToken, "newPassword123");

            mockMvc.perform(post("/api/v1/auth/setup-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("AUTH_PASSWORD_TOKEN_EXPIRED"));
        }
    }

    @Nested
    @DisplayName("MFA Login Flow Tests")
    class MfaLoginFlowTests {

        @Test
        @DisplayName("Admin without MFA should get MFA_SETUP_REQUIRED error")
        void adminWithoutMfaShouldGetSetupRequiredError() throws Exception {
            // Create admin user without MFA
            User adminUser = new User();
            adminUser.setEmail("admin@example.com");
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("adminpass123"));
            adminUser.setEnabled(true);
            adminUser.setFailedLoginCount(0);
            adminUser.setRole(adminRole);
            adminUser.setMfaEnabled(false);
            userRepository.save(adminUser);

            LoginRequest request = new LoginRequest("admin@example.com", "adminpass123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("AUTH_MFA_SETUP_REQUIRED"));
        }

        @Test
        @DisplayName("Admin with MFA should get MFA challenge response")
        void adminWithMfaShouldGetMfaChallenge() throws Exception {
            // Create admin user with MFA enabled
            User adminUser = new User();
            adminUser.setEmail("mfaadmin@example.com");
            adminUser.setUsername("mfaadmin");
            adminUser.setPassword(passwordEncoder.encode("adminpass123"));
            adminUser.setEnabled(true);
            adminUser.setFailedLoginCount(0);
            adminUser.setRole(adminRole);
            adminUser.setMfaEnabled(true);
            adminUser.setTotpSecret(mfaService.generateSecret());
            userRepository.save(adminUser);

            LoginRequest request = new LoginRequest("mfaadmin@example.com", "adminpass123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mfaToken").isNotEmpty())
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("Professor without MFA should login directly")
        void professorWithoutMfaShouldLoginDirectly() throws Exception {
            LoginRequest request = new LoginRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.mfaToken").doesNotExist());
        }

        @Test
        @DisplayName("Professor with MFA enabled should get MFA challenge")
        void professorWithMfaShouldGetMfaChallenge() throws Exception {
            // Enable MFA for the professor
            testUser.setMfaEnabled(true);
            testUser.setTotpSecret(mfaService.generateSecret());
            userRepository.save(testUser);

            LoginRequest request = new LoginRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mfaToken").isNotEmpty())
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }
    }

    @Nested
    @DisplayName("MFA Verify Endpoint Tests")
    class MfaVerifyEndpointTests {

        @Test
        @DisplayName("Should return 401 for invalid MFA token")
        void shouldReturn401ForInvalidMfaToken() throws Exception {
            MfaVerifyRequest request = new MfaVerifyRequest("invalid.token.here", "123456", MfaCodeType.TOTP);

            mockMvc.perform(post("/api/v1/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_MFA_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Should return 401 for invalid MFA code")
        void shouldReturn401ForInvalidMfaCode() throws Exception {
            // Create user with MFA
            User mfaUser = new User();
            mfaUser.setEmail("mfaverify@example.com");
            mfaUser.setUsername("mfaverify");
            mfaUser.setPassword(passwordEncoder.encode("password123"));
            mfaUser.setEnabled(true);
            mfaUser.setFailedLoginCount(0);
            mfaUser.setRole(userRole);
            mfaUser.setMfaEnabled(true);
            mfaUser.setTotpSecret(mfaService.generateSecret());
            mfaUser = userRepository.save(mfaUser);

            // Generate MFA token
            String mfaToken = mfaService.generateMfaToken(mfaUser);

            MfaVerifyRequest request = new MfaVerifyRequest(mfaToken, "000000", MfaCodeType.TOTP);

            mockMvc.perform(post("/api/v1/auth/mfa/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_MFA_INVALID_CODE"));
        }
    }

    @Nested
    @DisplayName("MFA Email Code Endpoint Tests")
    class MfaEmailCodeEndpointTests {

        @Test
        @DisplayName("Should return 401 for invalid MFA token in email code request")
        void shouldReturn401ForInvalidMfaToken() throws Exception {
            mockMvc.perform(post("/api/v1/auth/mfa/email-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mfaToken\": \"invalid.token.here\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value("AUTH_MFA_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("Should send email OTP with valid MFA token")
        void shouldSendEmailOtpWithValidToken() throws Exception {
            // Create user with MFA
            User mfaUser = new User();
            mfaUser.setEmail("emailotp@example.com");
            mfaUser.setUsername("emailotp");
            mfaUser.setPassword(passwordEncoder.encode("password123"));
            mfaUser.setEnabled(true);
            mfaUser.setFailedLoginCount(0);
            mfaUser.setRole(userRole);
            mfaUser.setMfaEnabled(true);
            mfaUser.setTotpSecret(mfaService.generateSecret());
            mfaUser = userRepository.save(mfaUser);

            // Generate MFA token
            String mfaToken = mfaService.generateMfaToken(mfaUser);

            mockMvc.perform(post("/api/v1/auth/mfa/email-code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mfaToken\": \"" + mfaToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sent").value(true));
        }
    }
}

