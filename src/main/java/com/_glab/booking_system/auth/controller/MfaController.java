package com._glab.booking_system.auth.controller;

import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.auth.exception.*;
import com._glab.booking_system.auth.model.RefreshToken;
import com._glab.booking_system.auth.repository.RefreshTokenRepository;
import com._glab.booking_system.auth.request.MfaDisableRequest;
import com._glab.booking_system.auth.request.MfaSetupVerifyRequest;
import com._glab.booking_system.auth.request.MfaVerifyRequest;
import com._glab.booking_system.auth.response.LoginResponse;
import com._glab.booking_system.auth.response.MfaChallengeResponse;
import com._glab.booking_system.auth.response.MfaSetupCompleteResponse;
import com._glab.booking_system.auth.response.MfaSetupResponse;
import com._glab.booking_system.auth.service.EmailOtpService;
import com._glab.booking_system.auth.service.JwtService;
import com._glab.booking_system.auth.service.MfaService;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Controller for MFA setup and verification endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Slf4j
public class MfaController {

    private final MfaService mfaService;
    private final EmailOtpService emailOtpService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    /**
     * Start MFA setup - generates a new TOTP secret and QR code.
     * Requires authentication.
     */
    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            log.warn("MFA setup attempt for user {} who already has MFA enabled", user.getEmail());
            throw new MfaAlreadyEnabledException("MFA is already enabled");
        }

        String secret = mfaService.generateSecret();
        String qrCodeDataUri = mfaService.generateQrCodeDataUri(user, secret);
        String manualEntryUri = mfaService.generateOtpAuthUri(user, secret);

        log.info("MFA setup initiated for user {}", user.getEmail());

        return ResponseEntity.ok(new MfaSetupResponse(secret, qrCodeDataUri, manualEntryUri));
    }

    /**
     * Complete MFA setup by verifying the first TOTP code.
     * Requires authentication.
     */
    @PostMapping("/setup/verify")
    public ResponseEntity<MfaSetupCompleteResponse> verifySetup(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MfaSetupVerifyRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            log.warn("MFA setup verify attempt for user {} who already has MFA enabled", user.getEmail());
            throw new MfaAlreadyEnabledException("MFA is already enabled");
        }

        // Verify the TOTP code with the provided secret
        if (!mfaService.verifyTotp(request.getSecret(), request.getCode())) {
            log.warn("MFA setup verification failed for user {} - invalid TOTP code", user.getEmail());
            throw new InvalidMfaCodeException("Invalid verification code");
        }

        // Generate backup codes
        MfaService.BackupCodesResult backupCodes = mfaService.generateBackupCodes();

        // Enable MFA for the user
        user.setTotpSecret(request.getSecret());
        user.setMfaEnabled(true);
        user.setMfaEnforcedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setBackupCodes(serializeBackupCodes(backupCodes.hashedCodes()));
        userRepository.save(user);

        log.info("MFA enabled for user {}", user.getEmail());

        return ResponseEntity.ok(new MfaSetupCompleteResponse(
                true,
                backupCodes.plainCodes(),
                "MFA has been enabled. Please save your backup codes in a safe place."
        ));
    }

    /**
     * Verify MFA during login (after password verification).
     * Public endpoint - uses mfaToken for authentication.
     */
    @PostMapping("/verify")
    public ResponseEntity<LoginResponse> verifyMfa(
            @RequestBody MfaVerifyRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String clientIp = getClientIp(httpRequest);

        // Parse and validate the MFA token
        MfaService.MfaTokenClaims claims;
        try {
            claims = mfaService.parseMfaToken(request.getMfaToken());
        } catch (Exception e) {
            log.warn("Invalid MFA token from IP {}", clientIp);
            throw new InvalidMfaTokenException("Invalid or expired MFA token");
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        boolean codeValid = switch (request.getCodeType()) {
            case TOTP -> mfaService.verifyTotp(user.getTotpSecret(), request.getCode());
            case EMAIL -> emailOtpService.verifyOtp(user, request.getCode());
            case BACKUP -> verifyAndConsumeBackupCode(user, request.getCode());
        };

        if (!codeValid) {
            log.warn("Invalid MFA code (type={}) for user {} from IP {}", request.getCodeType(), user.getEmail(), clientIp);
            throw new MfaVerificationFailedException("Invalid verification code");
        }

        // MFA verified - complete login
        emailOtpService.invalidateAllOtps(user);

        // Generate tokens
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

        log.info("MFA verification successful for user {} from IP {}", user.getEmail(), clientIp);

        LoginResponse.LoggedInUser loggedInUser = new LoginResponse.LoggedInUser(
                user.getId(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName().name() : "PROFESSOR"
        );

        return ResponseEntity.ok(new LoginResponse(accessToken, loggedInUser));
    }

    /**
     * Request email OTP as MFA fallback.
     * Public endpoint - uses mfaToken for authentication.
     */
    @PostMapping("/email-code")
    public ResponseEntity<Map<String, Object>> requestEmailCode(@RequestBody Map<String, String> request) {
        String mfaToken = request.get("mfaToken");

        MfaService.MfaTokenClaims claims;
        try {
            claims = mfaService.parseMfaToken(mfaToken);
        } catch (Exception e) {
            log.warn("Invalid MFA token in email-code request");
            throw new InvalidMfaTokenException("Invalid or expired MFA token");
        }

        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        boolean sent = emailOtpService.generateAndSendOtp(user);

        if (!sent) {
            log.debug("Email OTP rate-limited for user {}", user.getEmail());
            return ResponseEntity.ok(Map.of(
                    "sent", false,
                    "message", "Please wait before requesting another code"
            ));
        }

        log.info("Email OTP requested and sent to user {}", user.getEmail());

        return ResponseEntity.ok(Map.of(
                "sent", true,
                "message", "Verification code sent to your email"
        ));
    }

    /**
     * Disable MFA (only for Professors - Admins/Lab Managers cannot disable).
     * Requires authentication and current TOTP code.
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableMfa(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody MfaDisableRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        // Check if MFA is required for this role
        if (mfaService.isMfaRequired(user)) {
            RoleName role = user.getRole().getName();
            log.warn("MFA disable attempt blocked for user {} (role {} requires MFA)", user.getEmail(), role);
            throw new MfaRequiredException("MFA cannot be disabled for " + role + " accounts");
        }

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            log.warn("MFA disable attempt for user {} who doesn't have MFA enabled", user.getEmail());
            throw new MfaNotEnabledException("MFA is not enabled");
        }

        // Verify current TOTP code
        if (!mfaService.verifyTotp(user.getTotpSecret(), request.getCode())) {
            log.warn("MFA disable attempt failed for user {} - invalid TOTP code", user.getEmail());
            throw new InvalidMfaCodeException("Invalid verification code");
        }

        // Disable MFA
        user.setMfaEnabled(false);
        user.setTotpSecret(null);
        user.setBackupCodes(null);
        user.setMfaEnforcedAt(null);
        userRepository.save(user);

        log.info("MFA disabled for user {}", user.getEmail());

        return ResponseEntity.ok(Map.of(
                "mfaEnabled", false,
                "message", "MFA has been disabled"
        ));
    }

    /**
     * Get MFA status for the current user.
     * Requires authentication.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMfaStatus(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("User not found"));

        log.debug("MFA status check for user {}", user.getEmail());

        return ResponseEntity.ok(Map.of(
                "mfaEnabled", Boolean.TRUE.equals(user.getMfaEnabled()),
                "mfaRequired", mfaService.isMfaRequired(user),
                "canDisable", !mfaService.isMfaRequired(user)
        ));
    }

    // ==================== Helper Methods ====================

    private boolean verifyAndConsumeBackupCode(User user, String code) {
        List<String> hashedCodes = deserializeBackupCodes(user.getBackupCodes());
        if (hashedCodes == null || hashedCodes.isEmpty()) {
            return false;
        }

        int index = mfaService.verifyBackupCode(code, hashedCodes);
        if (index < 0) {
            return false;
        }

        // Remove the used backup code
        hashedCodes.set(index, null);
        user.setBackupCodes(serializeBackupCodes(hashedCodes));
        userRepository.save(user);

        log.info("Backup code used for user {}", user.getEmail());
        return true;
    }

    private String serializeBackupCodes(List<String> codes) {
        try {
            return objectMapper.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize backup codes", e);
        }
    }

    private List<String> deserializeBackupCodes(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize backup codes", e);
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

