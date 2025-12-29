package com._glab.booking_system.auth.controller;

import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.auth.exception.AccountDisabledException;
import com._glab.booking_system.auth.exception.AccountLockedException;
import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.auth.exception.InvalidRefreshTokenException;
import com._glab.booking_system.auth.exception.RefreshTokenExpiredException;
import com._glab.booking_system.auth.exception.RefreshTokenReuseException;
import com._glab.booking_system.auth.model.RefreshToken;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.request.LoginRequest;
import com._glab.booking_system.auth.request.SetupPasswordRequest;
import com._glab.booking_system.auth.response.LoginResponse;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.auth.service.PasswordSetupTokenService;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordSetupTokenService passwordSetupTokenService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody LoginRequest request,
                                                   HttpServletRequest httpRequest,
                                                   HttpServletResponse httpResponse) {
        String email = request.getEmail();
        String password = request.getPassword();

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationFailedException("Invalid credentials");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials"));

        // Account enabled check
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new AccountDisabledException("Account is disabled");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Lockout check
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
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
            } else if (failed >= 3) {
                user.setLockedUntil(now.plusMinutes(10));
            }

            userRepository.save(user);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        // Successful login: reset lockout counters
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLogin(now);
        userRepository.save(user);

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
                user.getRole() != null ? user.getRole().getName().name() : "USER"
        );
        LoginResponse responseBody = new LoginResponse(accessToken, loggedInUser);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse httpResponse) {

        if (refreshTokenCookie == null || refreshTokenCookie.isBlank()) {
            throw new InvalidRefreshTokenException("Refresh token is missing");
        }

        // Parse the JWT to extract jti
        String jti = jwtService.extractTokenId(refreshTokenCookie);

        // Look up refresh token in DB
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(jti)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        // Check if token is active
        if (!storedToken.isActive()) {
            if (storedToken.getRevokedAt() != null && storedToken.getReplacedByTokenId() != null) {
                // Token was already rotated - possible reuse attack
                throw new RefreshTokenReuseException("Refresh token reuse detected");
            }
            // Token expired
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }

        User user = storedToken.getUser();

        // Check if user is still enabled
        if (!Boolean.TRUE.equals(user.getEnabled())) {
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
                user.getRole() != null ? user.getRole().getName().name() : "USER"
        );

        return ResponseEntity.ok(new LoginResponse(newAccessToken, loggedInUser));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
            HttpServletResponse httpResponse) {

        // Revoke the refresh token if present
        if (refreshTokenCookie != null && !refreshTokenCookie.isBlank()) {
            try {
                String jti = jwtService.extractTokenId(refreshTokenCookie);
                refreshTokenRepository.findByTokenId(jti).ifPresent(token -> {
                    if (token.getRevokedAt() == null) {
                        token.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
                        refreshTokenRepository.save(token);
                    }
                });
            } catch (Exception ignored) {
                // Token parsing failed - just clear the cookie
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
            HttpServletResponse httpResponse) {

        String token = request.getToken();
        String newPassword = request.getNewPassword();

        if (token == null || token.isBlank()) {
            throw new AuthenticationFailedException("Token is required");
        }
        if (newPassword == null || newPassword.isBlank()) {
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
        userRepository.save(user);

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
                user.getRole() != null ? user.getRole().getName().name() : "USER"
        );

        return ResponseEntity.ok(new LoginResponse(accessToken, loggedInUser));
    }
}
