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
                                               boolean isRecurring, int occurrenceCount, java.util.UUID reservationId) {
        String subject = "5GLab Booking - Reservation Request Submitted";
        
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/reservations/%s", frontendUrl, reservationId);
        String generalLink = String.format("%s/reservations", frontendUrl);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "Your reservation request has been submitted and is pending review.\n\n" +
                "You will receive another email once a Lab Manager reviews your request.\n\n" +
                "View your reservation: %s\n" +
                "View all reservations: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                userName, reservationLink, generalLink
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
                                                boolean isRecurring, int occurrenceCount, java.util.UUID reservationId) {
        String subject = "5GLab Booking - New Reservation Request";
        
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/manager/reservations/%s", frontendUrl, reservationId);
        String generalLink = String.format("%s/manager/reservations", frontendUrl);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A new reservation request has been submitted for your lab.\n\n" +
                "View this reservation: %s\n" +
                "View all pending reservations: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, reservationLink, generalLink
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
                "%s%s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                userName, statusMessage, reasonInfo
        );

        sendEmail(userEmail, subject, body);
        log.info("Reservation status change ({}) email sent to {}", newStatus, userEmail);
    }

    // ==================== Edit Proposal Emails ====================

    /**
     * Send email to professor when lab manager edits their reservation.
     *
     * @param professorEmail Professor's email address
     * @param professorName Professor's full name
     * @param managerName   Lab manager's name
     * @param labName       Name of the lab
     * @param reservationId Reservation ID
     */
    @Async
    public void sendReservationEditProposalEmailToProfessor(String professorEmail, String professorName,
                                                             String managerName, String labName,
                                                             java.util.UUID reservationId) {
        String subject = "5GLab Booking - Reservation Edit Proposal";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/reservations/%s?editProposal=true", frontendUrl, reservationId);
        String generalLink = String.format("%s/reservations", frontendUrl);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A Lab Manager has proposed changes to your reservation.\n\n" +
                "Please review the proposed changes and approve or reject them.\n\n" +
                "View edit proposal: %s\n" +
                "View all reservations: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                professorName, reservationLink, generalLink
        );

        sendEmail(professorEmail, subject, body);
        log.info("Edit proposal email sent to professor {}", professorEmail);
    }

    /**
     * Send email to lab manager when professor edits their APPROVED reservation (requires re-approval).
     *
     * @param managerEmail  Lab manager's email address
     * @param managerName    Lab manager's name
     * @param professorName  Professor's name
     * @param labName        Name of the lab
     * @param reservationId  Reservation ID
     */
    @Async
    public void sendReservationEditProposalEmailToManager(String managerEmail, String managerName,
                                                           String professorName, String labName,
                                                           java.util.UUID reservationId) {
        String subject = "5GLab Booking - Reservation Edit Requires Re-approval";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/manager/reservations/%s?editProposal=true", frontendUrl, reservationId);
        String generalLink = String.format("%s/manager/reservations", frontendUrl);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A professor has edited their approved reservation.\n\n" +
                "This edit requires your re-approval before it can take effect.\n\n" +
                "View edit proposal: %s\n" +
                "View all pending reservations: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, reservationLink, generalLink
        );

        sendEmail(managerEmail, subject, body);
        log.info("Edit proposal email sent to manager {}", managerEmail);
    }

    /**
     * Send notification email to lab manager when professor edits their PENDING reservation (changes applied automatically).
     *
     * @param managerEmail  Lab manager's email address
     * @param managerName   Lab manager's name
     * @param professorName Professor's name
     * @param labName       Name of the lab
     * @param reservationId Reservation ID
     */
    @Async
    public void sendReservationUpdatedEmailToManager(String managerEmail, String managerName,
                                                      String professorName, String labName,
                                                      java.util.UUID reservationId) {
        String subject = "5GLab Booking - Reservation Updated";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/manager/reservations/%s", frontendUrl, reservationId);
        String generalLink = String.format("%s/manager/reservations", frontendUrl);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A professor has updated their pending reservation request.\n\n" +
                "The changes have been applied automatically. Please review the updated request.\n\n" +
                "View reservation: %s\n" +
                "View all pending reservations: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, reservationLink, generalLink
        );

        sendEmail(managerEmail, subject, body);
        log.info("Reservation updated email sent to manager {}", managerEmail);
    }

    /**
     * Send email to lab manager when professor approves their edit.
     *
     * @param managerEmail  Lab manager's email address
     * @param managerName   Lab manager's name
     * @param professorName Professor's name
     * @param labName       Name of the lab
     * @param reservationId Reservation ID
     */
    @Async
    public void sendEditApprovedByProfessorEmail(String managerEmail, String managerName,
                                                  String professorName, String labName,
                                                  java.util.UUID reservationId) {
        String subject = "5GLab Booking - Edit Approved by Professor";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/manager/reservations/%s", frontendUrl, reservationId);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A professor has approved your edit proposal for their reservation.\n\n" +
                "The changes have been applied to the reservation.\n\n" +
                "View reservation: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, reservationLink
        );

        sendEmail(managerEmail, subject, body);
        log.info("Edit approved by professor email sent to manager {}", managerEmail);
    }

    /**
     * Send email to lab manager when professor rejects their edit.
     *
     * @param managerEmail  Lab manager's email address
     * @param managerName   Lab manager's name
     * @param professorName Professor's name
     * @param labName       Name of the lab
     * @param reservationId Reservation ID
     * @param reason        Optional reason for rejection
     */
    @Async
    public void sendEditRejectedByProfessorEmail(String managerEmail, String managerName,
                                                  String professorName, String labName,
                                                  java.util.UUID reservationId, String reason) {
        String subject = "5GLab Booking - Edit Rejected by Professor";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/manager/reservations/%s", frontendUrl, reservationId);
        
        String reasonInfo = (reason != null && !reason.isBlank()) 
                ? String.format("\nReason provided: %s\n", reason) 
                : "";
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A professor has rejected your edit proposal for their reservation.\n\n" +
                "The original reservation values have been restored.%s\n" +
                "View reservation: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                managerName, reasonInfo, reservationLink
        );

        sendEmail(managerEmail, subject, body);
        log.info("Edit rejected by professor email sent to manager {}", managerEmail);
    }

    /**
     * Send email to professor when lab manager approves their edit.
     *
     * @param professorEmail Professor's email address
     * @param professorName  Professor's full name
     * @param managerName    Lab manager's name
     * @param labName        Name of the lab
     * @param reservationId  Reservation ID
     */
    @Async
    public void sendEditApprovedByManagerEmail(String professorEmail, String professorName,
                                               String managerName, String labName,
                                               java.util.UUID reservationId) {
        String subject = "5GLab Booking - Edit Approved";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/reservations/%s", frontendUrl, reservationId);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A Lab Manager has approved your edit proposal for your reservation.\n\n" +
                "The changes have been applied to your reservation.\n\n" +
                "View reservation: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                professorName, reservationLink
        );

        sendEmail(professorEmail, subject, body);
        log.info("Edit approved by manager email sent to professor {}", professorEmail);
    }

    /**
     * Send email to professor when lab manager rejects their edit.
     *
     * @param professorEmail Professor's email address
     * @param professorName  Professor's full name
     * @param managerName    Lab manager's name
     * @param labName        Name of the lab
     * @param reservationId  Reservation ID
     * @param reason         Optional reason for rejection
     */
    @Async
    public void sendEditRejectedByManagerEmail(String professorEmail, String professorName,
                                               String managerName, String labName,
                                               java.util.UUID reservationId, String reason) {
        String subject = "5GLab Booking - Edit Rejected";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/reservations/%s", frontendUrl, reservationId);
        
        String reasonInfo = (reason != null && !reason.isBlank()) 
                ? String.format("\nReason provided: %s\n", reason) 
                : "";
        
        String body = String.format(
                "Hello %s,\n\n" +
                "A Lab Manager has rejected your edit proposal for your reservation.\n\n" +
                "The original reservation values have been restored.%s\n" +
                "View reservation: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                professorName, reasonInfo, reservationLink
        );

        sendEmail(professorEmail, subject, body);
        log.info("Edit rejected by manager email sent to professor {}", professorEmail);
    }

    /**
     * Send email to professor when lab manager removes (cancels) their approved reservation.
     *
     * @param professorEmail Professor's email address
     * @param professorName  Professor's full name
     * @param managerName    Lab manager's name
     * @param labName        Name of the lab
     * @param startTime      Reservation start time
     * @param endTime        Reservation end time
     * @param reservationId  Reservation ID
     */
    @Async
    public void sendReservationRemovedEmail(String professorEmail, String professorName,
                                            String managerName, String labName,
                                            String startTime, String endTime,
                                            java.util.UUID reservationId) {
        String subject = "5GLab Booking - Reservation Cancelled";
        String frontendUrl = appProperties.getFrontend().getUrl();
        String reservationLink = String.format("%s/reservations/%s", frontendUrl, reservationId);
        
        String body = String.format(
                "Hello %s,\n\n" +
                "Your approved reservation has been cancelled by a Lab Manager.\n\n" +
                "View reservation: %s\n\n" +
                "Best regards,\n" +
                "5GLab Booking System",
                professorName, reservationLink
        );

        sendEmail(professorEmail, subject, body);
        log.info("Reservation removed email sent to professor {}", professorEmail);
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
