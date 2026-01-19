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

    // ==================== Reservation Emails ====================

    /**
     * Send confirmation email to user when they submit a reservation.
     *
     * @param userEmail     User's email address
     * @param userName      User's full name
     * @param labName       Name of the lab
     * @param startTime     Reservation start time (formatted)
     * @param endTime       Reservation end time (formatted)
     * @param isRecurring   Whether this is a recurring reservation
     * @param occurrenceCount Number of occurrences (for recurring)
     */
    @Async
    public void sendReservationSubmittedEmail(String userEmail, String userName, String labName,
                                               String startTime, String endTime, 
                                               boolean isRecurring, int occurrenceCount) {
        String subject = "5GLab Booking - Reservation Request Submitted";
        
        String recurringInfo = isRecurring 
                ? String.format("\nThis is a recurring reservation with %d occurrences.", occurrenceCount)
                : "";
        
        String body = String.format(
                "Hello %s,\n\n" +
                "Your reservation request has been submitted and is pending review.\n\n" +
                "Details:\n" +
                "- Lab: %s\n" +
                "- Start: %s\n" +
                "- End: %s%s\n\n" +
                "You will receive another email once a Lab Manager reviews your request.\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                userName, labName, startTime, endTime, recurringInfo
        );

        sendEmail(userEmail, subject, body);
        log.info("Reservation submitted email sent to {}", userEmail);
    }

    /**
     * Send notification email to lab manager when a new reservation request is submitted.
     *
     * @param managerEmail  Lab manager's email address
     * @param managerName   Lab manager's name
     * @param labName       Name of the lab
     * @param requesterName Name of the user who submitted the request
     * @param startTime     Reservation start time (formatted)
     * @param endTime       Reservation end time (formatted)
     * @param isRecurring   Whether this is a recurring reservation
     * @param occurrenceCount Number of occurrences (for recurring)
     */
    @Async
    public void sendNewReservationRequestEmail(String managerEmail, String managerName, String labName,
                                                String requesterName, String startTime, String endTime,
                                                boolean isRecurring, int occurrenceCount) {
        String subject = "5GLab Booking - New Reservation Request for " + labName;
        
        String recurringInfo = isRecurring 
                ? String.format("\nThis is a recurring reservation with %d occurrences.", occurrenceCount)
                : "";
        
        String frontendUrl = appProperties.getFrontend().getUrl();
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A new reservation request has been submitted for your lab.\n\n" +
                "Details:\n" +
                "- Lab: %s\n" +
                "- Requested by: %s\n" +
                "- Start: %s\n" +
                "- End: %s%s\n\n" +
                "Please log in to review and approve/reject this request:\n" +
                "%s/manager/reservations\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, labName, requesterName, startTime, endTime, recurringInfo, frontendUrl
        );

        sendEmail(managerEmail, subject, body);
        log.info("New reservation request email sent to manager {}", managerEmail);
    }

    /**
     * Send notification email when a reservation status changes.
     *
     * @param userEmail   User's email address
     * @param userName    User's full name
     * @param labName     Name of the lab
     * @param startTime   Reservation start time (formatted)
     * @param endTime     Reservation end time (formatted)
     * @param newStatus   New status (APPROVED, REJECTED, CANCELLED)
     * @param reason      Optional reason for the status change
     */
    @Async
    public void sendReservationStatusChangeEmail(String userEmail, String userName, String labName,
                                                  String startTime, String endTime, 
                                                  String newStatus, String reason) {
        String subject = String.format("5GLab Booking - Reservation %s", newStatus);
        
        String statusMessage = switch (newStatus.toUpperCase()) {
            case "APPROVED" -> "Your reservation has been approved!";
            case "REJECTED" -> "Unfortunately, your reservation has been rejected.";
            case "CANCELLED" -> "Your reservation has been cancelled.";
            default -> "Your reservation status has been updated to: " + newStatus;
        };
        
        String reasonInfo = (reason != null && !reason.isBlank()) 
                ? String.format("\nReason: %s", reason) 
                : "";
        
        String body = String.format(
                "Hello %s,\n\n" +
                "%s\n\n" +
                "Details:\n" +
                "- Lab: %s\n" +
                "- Start: %s\n" +
                "- End: %s%s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                userName, statusMessage, labName, startTime, endTime, reasonInfo
        );

        sendEmail(userEmail, subject, body);
        log.info("Reservation status change ({}) email sent to {}", newStatus, userEmail);
    }

    // ==================== Helper Methods ====================

    /**
     * Build the password setup URL for the frontend.
     */
    private String buildSetupUrl(String token) {
        String frontendUrl = appProperties.getFrontend().getUrl();
        return String.format("%s/setup-password?token=%s", frontendUrl, token);
    }
}
