package com._glab.booking_system.auth.controller;

import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.auth.exception.AccountDisabledException;
import com._glab.booking_system.auth.exception.AccountLockedException;
import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.auth.exception.InvalidRefreshTokenException;
import com._glab.booking_system.auth.exception.MfaSetupRequiredException;
import com._glab.booking_system.auth.exception.RefreshTokenExpiredException;
import com._glab.booking_system.auth.exception.RefreshTokenReuseException;
import com._glab.booking_system.auth.model.RefreshToken;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.request.LoginRequest;
import com._glab.booking_system.auth.request.SetupPasswordRequest;
import com._glab.booking_system.auth.response.LoginResponse;
import com._glab.booking_system.auth.response.MfaChallengeResponse;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.auth.service.MfaService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordSetupTokenService passwordSetupTokenService;
    private final MfaService mfaService;

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        String email = request.getEmail();
        String password = request.getPassword();

        String clientIp = getClientIp(httpRequest);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Login attempt with empty credentials from IP {}", clientIp);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt for unknown email {} from IP {}", email, clientIp);
                    return new AuthenticationFailedException("Invalid credentials");
                });

        // Account enabled check
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Login attempt for disabled account {} from IP {}", email, clientIp);
            throw new AccountDisabledException("Account is disabled");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            log.warn("Login attempt for locked account {} from IP {}", email, clientIp);
            throw new AccountLockedException("Account is locked until " + user.getLockedUntil());
        }

        // If lockout has expired, reset the failed counter to allow fresh lockout tiers
        boolean lockoutExpired = user.getLockedUntil() != null && user.getLockedUntil().isBefore(now);
        if (lockoutExpired) {
            user.setFailedLoginCount(0);
            user.setLockedUntil(null);
        }

        // Password verification
        if (!passwordEncoder.matches(password, user.getPassword())) {
            int failed = user.getFailedLoginCount() != null ? user.getFailedLoginCount() : 0;
            failed++;
            user.setFailedLoginCount(failed);

            // Tiered lockout: 3 fails -> 10 minutes, 6 fails -> 30 minutes
            if (failed >= 6) {
                user.setLockedUntil(now.plusMinutes(30));
                log.warn("Account {} locked for 30 minutes after {} failed attempts from IP {}", email, failed, clientIp);
            } else if (failed >= 3) {
                user.setLockedUntil(now.plusMinutes(10));
                log.warn("Account {} locked for 10 minutes after {} failed attempts from IP {}", email, failed, clientIp);
            } else {
                log.warn("Failed login attempt {} for {} from IP {}", failed, email, clientIp);
            }

            userRepository.save(user);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        // Password is correct - reset lockout counters
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);

        // Check if MFA is required but not set up
        if (mfaService.needsMfaSetup(user)) {
            userRepository.save(user);
            log.warn("MFA setup required for user {} from IP {}", email, clientIp);
            throw new MfaSetupRequiredException("MFA setup is required for your account. Please set up MFA first.");
        }

        // Check if MFA is enabled - return challenge instead of tokens
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            userRepository.save(user);
            String mfaToken = mfaService.generateMfaToken(user);
            log.info("MFA challenge issued for user {} from IP {}", email, clientIp);
            return ResponseEntity.ok(new MfaChallengeResponse(mfaToken));
        }

        // No MFA - complete login
        user.setLastLogin(now);
        user.setLastLoginIp(clientIp);
        userRepository.save(user);

        log.info("User {} logged in from IP {}", user.getEmail(), clientIp);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        JwtService.RefreshTokenResult refreshResult = jwtService.generateRefreshToken(user);

        // Persist refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenId(refreshResult.jti());
        refreshToken.setExpiresAt(OffsetDateTime.ofInstant(refreshResult.expiresAt().toInstant(), ZoneOffset.UTC));
        refreshTokenRepository.save(refreshToken);

        // Set refresh token cookie (httpOnly, Secure)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshResult.token())
                .httpOnly(true)
                .secure(false) // set to true when you have HTTPS
                .path("/api/v1/auth/refresh")
                .maxAge(refreshResult.expiresAt().toInstant().getEpochSecond() - now.toInstant().getEpochSecond())
                .sameSite("Strict")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Build response
        LoginResponse.LoggedInUser loggedInUser = new LoginResponse.LoggedInUser(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName().name() : "PROFESSOR"
        );
        LoginResponse responseBody = new LoginResponse(accessToken, loggedInUser);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = getClientIp(httpRequest);

        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            log.warn("Token refresh attempt without cookie from IP {}", clientIp);
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        // Parse the JWT to extract jti
        String jti = jwtService.extractTokenId(refreshTokenCookie);

        // Look up refresh token in DB
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(jti)
                .orElseThrow(() -> {
                    log.warn("Token refresh attempt with unknown token from IP {}", clientIp);
                    return new InvalidRefreshTokenException("Refresh token not found");
                });

        User user = storedToken.getUser();

        // Check if token is active
        if (!storedToken.isActive()) {
            if (storedToken.getRevokedAt() != null && storedToken.getReplacedByTokenId() != null) {
                // Token was already rotated - possible reuse attack
                log.error("SECURITY: Refresh token reuse detected for user {} from IP {}", user.getEmail(), clientIp);
                throw new RefreshTokenReuseException("Refresh token reuse detected");
            }
            // Token expired
            log.debug("Expired token refresh attempt for user {} from IP {}", user.getEmail(), clientIp);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        // Check if user is still enabled
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            log.warn("Token refresh attempt for disabled account {} from IP {}", user.getEmail(), clientIp);
            throw new AccountDisabledException("Account is disabled");
        }

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        JwtService.RefreshTokenResult newRefreshResult = jwtService.generateRefreshToken(user);

        // Revoke old token and link to new one
        storedToken.revoke(newRefreshResult.jti());
        refreshTokenRepository.save(storedToken);

        // Persist new refresh token
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenId(newRefreshResult.jti());
        newRefreshToken.setExpiresAt(OffsetDateTime.ofInstant(newRefreshResult.expiresAt().toInstant(), ZoneOffset.UTC));
        refreshTokenRepository.save(newRefreshToken);

        log.debug("Token refreshed for user {} from IP {}", user.getEmail(), clientIp);

        // Set new refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshResult.token())
                .httpOnly(true)
                .secure(false) // set to true when you have HTTPS
                .path("/api/v1/auth/refresh")
                .maxAge(jwtProperties.getRefreshTokenExpiry().toSeconds())
                .sameSite("Strict")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Build response
        LoginResponse.LoggedInUser loggedInUser = new LoginResponse.LoggedInUser(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName().name() : "PROFESSOR"
        );

        return ResponseEntity.ok(new LoginResponse(newAccessToken, loggedInUser));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = getClientIp(httpRequest);

        // Revoke the refresh token if present
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            try {
                String jti = jwtService.extractTokenId(refreshTokenCookie);
                refreshTokenRepository.findByTokenId(jti).ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        refreshTokenRepository.save(token);
                        log.info("User {} logged out from IP {}", token.getUser().getEmail(), clientIp);
                    }
                });
            } catch (Exception e) {
                log.debug("Logout with invalid token from IP {}", clientIp);
            }
        }

        // Clear the refresh token cookie
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/setup-password")
    public ResponseEntity<LoginResponse> setupPassword(
            @RequestBody SetupPasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = getClientIp(httpRequest);
        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (token == null || token.isBlank()) {
            log.warn("Password setup attempt without token from IP {}", clientIp);
            throw new AuthenticationFailedException("Token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
            log.warn("Password setup attempt without password from IP {}", clientIp);
            throw new AuthenticationFailedException("New password is required");
        }

        // Validate and consume the token (throws InvalidPasswordSetupTokenException or ExpiredPasswordSetupTokenException)
        User user = passwordSetupTokenService.validateAndConsumeToken(token);

        // Set the new password and enable the account
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        user.setPasswordChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginIp(clientIp);
        userRepository.save(user);

        log.info("Password setup completed for user {} from IP {}", user.getEmail(), clientIp);

        // Auto-login: generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        JwtService.RefreshTokenResult refreshResult = jwtService.generateRefreshToken(user);

        // Persist refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenId(refreshResult.jti());
        refreshToken.setExpiresAt(OffsetDateTime.ofInstant(refreshResult.expiresAt().toInstant(), ZoneOffset.UTC));
        refreshTokenRepository.save(refreshToken);

        // Set refresh token cookie
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshResult.token())
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(jwtProperties.getRefreshTokenExpiry().toSeconds())
                .sameSite("Strict")
                .build();
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Build response
        LoginResponse.LoggedInUser loggedInUser = new LoginResponse.LoggedInUser(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName().name() : "PROFESSOR"
        );

        return ResponseEntity.ok(new LoginResponse(accessToken, loggedInUser));
    }

    /**
     * Extracts the client IP address from the request.
     * Handles X-Forwarded-For header for clients behind proxies/load balancers.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For can contain multiple IPs; first one is the original client
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
