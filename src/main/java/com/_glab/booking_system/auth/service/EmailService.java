package com._glab.booking_system.auth.service;

import com._glab.booking_system.auth.config.AppProperties;
import com._glab.booking_system.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Centralized email service for all email sending operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties appProperties;

    /**
     * Send account setup email with password setup link.
     *
     * @param user  The user to send the email to
     * @param token The password setup token
     */
    @Async
    public void sendAccountSetupEmail(User user, String token) {
        String setupUrl = buildSetupUrl(token);
        String subject = "5GLab Booking - Complete Your Account Setup";
        String body = String.format(
                "Hello %s %s,\n\n" +
                "An account has been created for you in the 5GLab Booking System.\n\n" +
                "Please click the link below to set up your password and activate your account:\n\n" +
                "%s\n\n" +
                "This link will expire in 48 hours.\n\n" +
                "If you did not expect this email, please ignore it.\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                user.getFirstName(),
                user.getLastName(),
                setupUrl
        );

        sendEmail(user.getEmail(), subject, body);
        log.info("Account setup email sent to {}", user.getEmail());
    }

    /**
     * Send OTP verification email.
     *
     * @param email The recipient email address
     * @param otp   The OTP code
     * @param expiryMinutes How long the OTP is valid
     */
    @Async
    public void sendOtpEmail(String email, String otp, long expiryMinutes) {
        String subject = "5GLab Booking - Your Verification Code";
        String body = String.format(
                "Your verification code is: %s\n\n" +
                "This code will expire in %d minutes.\n\n" +
                "If you did not request this code, please ignore this email.",
                otp,
                expiryMinutes
        );

        sendEmail(email, subject, body);
        log.debug("OTP email sent to {}", email);
    }

    /**
     * Send a generic email.
     *
     * @param to      Recipient email address
     * @param subject Email subject
     * @param body    Email body (plain text)
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(appProperties.getMail().getFrom());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.debug("Email sent to {} with subject: {}", to, subject);
        } catch (MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw e;
        }
    }

    /**
     * Build the password setup URL for the frontend.
     */
    private String buildSetupUrl(String token) {
        String frontendUrl = appProperties.getFrontend().getUrl();
        return String.format("%s/setup-password?token=%s", frontendUrl, token);
    }
}
