package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.model.EmailOtp;
import com._glab.booking_system.auth.repository.EmailOtpRepository;
import com._glab.booking_system.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Service for email-based OTP as MFA fallback.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOtpService {

    private static final Duration OTP_EXPIRY = Duration.ofMinutes(10);
    private static final Duration RATE_LIMIT_DURATION = Duration.ofSeconds(60);
    private static final int OTP_LENGTH = 6;

    private final EmailOtpRepository emailOtpRepository;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generate and send an OTP to the user's email.
     * 
     * @param user The user to send the OTP to
     * @return true if OTP was sent, false if rate limited
     */
    @Transactional
    public boolean generateAndSendOtp(User user) {
        // Check rate limiting
        if (isRateLimited(user)) {
            log.warn("OTP request rate limited for user {}", user.getEmail());
            return false;
        }

        // Generate OTP
        String otp = generateOtp();
        String hash = hashCode(otp);

        // Save to database
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.setUser(user);
        emailOtp.setCodeHash(hash);
        emailOtp.setExpiresAt(OffsetDateTime.now().plus(OTP_EXPIRY));
        emailOtpRepository.save(emailOtp);

        // Send email asynchronously
        sendOtpEmail(user.getEmail(), otp);

        log.info("OTP sent to user {}", user.getEmail());
        return true;
    }

    /**
     * Verify an OTP code for the user.
     * 
     * @param user The user
     * @param code The OTP code to verify
     * @return true if the code is valid
     */
    @Transactional
    public boolean verifyOtp(User user, String code) {
        if (code == null || code.length() != OTP_LENGTH) {
            return false;
        }

        String hash = hashCode(code);
        OffsetDateTime now = OffsetDateTime.now();

        Optional<EmailOtp> otpOpt = emailOtpRepository.findValidOtp(user, hash, now);

        if (otpOpt.isPresent()) {
            EmailOtp otp = otpOpt.get();
            otp.markUsed();
            emailOtpRepository.save(otp);
            log.info("OTP verified for user {}", user.getEmail());
            return true;
        }

        log.debug("OTP verification failed for user {}", user.getEmail());
        return false;
    }

    /**
     * Invalidate all pending OTPs for a user (e.g., after successful login).
     */
    @Transactional
    public void invalidateAllOtps(User user) {
        int count = emailOtpRepository.invalidateAllForUser(user, OffsetDateTime.now());
        if (count > 0) {
            log.debug("Invalidated {} pending OTPs for user {}", count, user.getEmail());
        }
    }

    /**
     * Clean up expired OTPs.
     * Should be called periodically by a scheduled job.
     */
    @Transactional
    public int cleanupExpiredOtps() {
        OffsetDateTime cutoff = OffsetDateTime.now().minus(OTP_EXPIRY.multipliedBy(2));
        int deleted = emailOtpRepository.deleteExpiredOtps(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} expired email OTPs", deleted);
        }
        return deleted;
    }

    /**
     * Check if the user is rate-limited from requesting new OTPs.
     */
    private boolean isRateLimited(User user) {
        Optional<EmailOtp> lastOtp = emailOtpRepository.findTopByUserOrderByCreatedAtDesc(user);
        
        if (lastOtp.isEmpty()) {
            return false;
        }

        OffsetDateTime lastCreated = lastOtp.get().getCreatedAt();
        if (lastCreated == null) {
            return false;
        }

        return lastCreated.plus(RATE_LIMIT_DURATION).isAfter(OffsetDateTime.now());
    }

    /**
     * Generate a random 6-digit OTP.
     */
    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Hash the OTP code using SHA-256.
     */
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Send OTP via email.
     */
    @Async
    protected void sendOtpEmail(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("5GLab Booking - Your Verification Code");
            message.setText(String.format(
                    "Your verification code is: %s\n\n" +
                    "This code will expire in %d minutes.\n\n" +
                    "If you did not request this code, please ignore this email.",
                    otp,
                    OTP_EXPIRY.toMinutes()
            ));

            mailSender.send(message);
            log.debug("OTP email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", email, e);
        }
    }
}

