package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.JwtKeyProvider;
import com._glab.booking_system.auth.config.JwtProperties;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * Service for Multi-Factor Authentication (MFA) operations.
 * Handles TOTP generation/verification, MFA tokens, and role-based enforcement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private static final String MFA_TOKEN_ISSUER = "booking-system-mfa";
    private static final Duration MFA_TOKEN_EXPIRY = Duration.ofMinutes(5);
    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final JwtKeyProvider keyProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    // ==================== Role Enforcement ====================

    /**
     * Check if MFA is required for the user based on their role.
     * Admins and Lab Managers must have MFA enabled.
     */
    public boolean isMfaRequired(User user) {
        if (user.getRole() == null) {
            return false;
        }
        RoleName role = user.getRole().getName();
        boolean required = role == RoleName.ADMIN || role == RoleName.LAB_MANAGER;
        log.trace("MFA required check for user {} (role={}): {}", user.getEmail(), role, required);
        return required;
    }

    /**
     * Check if the user needs to set up MFA before they can proceed.
     * Returns true if MFA is required by role but not yet enabled.
     */
    public boolean needsMfaSetup(User user) {
        boolean needsSetup = isMfaRequired(user) && !Boolean.TRUE.equals(user.getMfaEnabled());
        if (needsSetup) {
            log.debug("User {} needs MFA setup (role requires MFA but not enabled)", user.getEmail());
        }
        return needsSetup;
    }

    // ==================== TOTP Secret Management ====================

    /**
     * Generate a new TOTP secret for MFA setup.
     */
    public String generateSecret() {
        String secret = secretGenerator.generate();
        log.debug("Generated new TOTP secret");
        return secret;
    }

    /**
     * Generate a QR code data URI for the authenticator app.
     * 
     * @param user The user setting up MFA
     * @param secret The TOTP secret
     * @return Base64-encoded PNG image as data URI
     */
    public String generateQrCodeDataUri(User user, String secret) {
        QrData qrData = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("5GLab Booking")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = qrGenerator.generate(qrData);
            return getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            log.error("Failed to generate QR code for user {}", user.getEmail(), e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate the otpauth:// URI for manual entry in authenticator apps.
     */
    public String generateOtpAuthUri(User user, String secret) {
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                "5GLab Booking",
                user.getEmail(),
                secret,
                "5GLab Booking"
        );
    }

    // ==================== TOTP Verification ====================

    /**
     * Verify a TOTP code against the user's secret.
     * 
     * @param secret The user's TOTP secret
     * @param code The 6-digit code to verify
     * @return true if the code is valid
     */
    public boolean verifyTotp(String secret, String code) {
        if (secret == null || code == null) {
            log.debug("TOTP verification failed: secret or code is null");
            return false;
        }
        boolean valid = codeVerifier.isValidCode(secret, code);
        log.debug("TOTP verification result: {}", valid ? "success" : "failed");
        return valid;
    }

    // ==================== Backup Codes ====================

    /**
     * Generate backup codes for MFA recovery.
     * Returns a list of plain-text codes (to show to user) and hashed codes (to store).
     */
    public BackupCodesResult generateBackupCodes() {
        List<String> plainCodes = new ArrayList<>();
        List<String> hashedCodes = new ArrayList<>();

        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            String code = generateBackupCode();
            plainCodes.add(code);
            hashedCodes.add(passwordEncoder.encode(code));
        }

        log.debug("Generated {} backup codes", BACKUP_CODE_COUNT);
        return new BackupCodesResult(plainCodes, hashedCodes);
    }

    private String generateBackupCode() {
        // Generate alphanumeric code (e.g., "A1B2-C3D4")
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude similar chars (0/O, 1/I)
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < BACKUP_CODE_LENGTH; i++) {
            if (i == BACKUP_CODE_LENGTH / 2) {
                code.append("-");
            }
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    /**
     * Verify a backup code against stored hashed codes.
     * 
     * @param code The backup code to verify
     * @param hashedCodes JSON array of hashed backup codes
     * @return The index of the matched code, or -1 if not found
     */
    public int verifyBackupCode(String code, List<String> hashedCodes) {
        if (code == null || hashedCodes == null) {
            log.debug("Backup code verification failed: code or hashedCodes is null");
            return -1;
        }

        String normalizedCode = code.toUpperCase().replace("-", "");
        
        for (int i = 0; i < hashedCodes.size(); i++) {
            String hash = hashedCodes.get(i);
            if (hash != null && passwordEncoder.matches(normalizedCode, hash)) {
                log.debug("Backup code verified at index {}", i);
                return i;
            }
        }
        
        log.debug("Backup code verification failed: no matching code found");
        return -1;
    }

    // ==================== MFA Token (Intermediate JWT) ====================

    /**
     * Generate a short-lived MFA token after password verification.
     * This token proves the password was correct and awaits 2FA.
     */
    public String generateMfaToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + MFA_TOKEN_EXPIRY.toMillis());

        String token = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("mfaPending", true)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiry)
                .issuer(MFA_TOKEN_ISSUER)
                .signWith(keyProvider.getPrivateKey())
                .compact();

        log.debug("Generated MFA token for user {} (expires in {} minutes)", 
                user.getEmail(), MFA_TOKEN_EXPIRY.toMinutes());
        return token;
    }

    /**
     * Parse and validate an MFA token.
     * 
     * @param token The MFA token
     * @return The user ID from the token
     * @throws RuntimeException if the token is invalid or expired
     */
    public MfaTokenClaims parseMfaToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keyProvider.getPublicKey())
                    .requireIssuer(MFA_TOKEN_ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Boolean mfaPending = claims.get("mfaPending", Boolean.class);
            if (!Boolean.TRUE.equals(mfaPending)) {
                throw new RuntimeException("Invalid MFA token: not an MFA pending token");
            }

            return new MfaTokenClaims(
                    claims.getSubject(),
                    claims.get("userId", Integer.class)
            );
        } catch (ExpiredJwtException e) {
            log.debug("MFA token expired: {}", e.getMessage());
            throw new RuntimeException("MFA token has expired", e);
        } catch (JwtException e) {
            log.debug("MFA token invalid: {}", e.getMessage());
            throw new RuntimeException("Invalid MFA token", e);
        }
    }

    // ==================== Result Records ====================

    public record BackupCodesResult(List<String> plainCodes, List<String> hashedCodes) {}

    public record MfaTokenClaims(String email, Integer userId) {}
}

