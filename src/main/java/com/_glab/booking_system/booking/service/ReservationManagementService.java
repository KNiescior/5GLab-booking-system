package com._glab.booking_system.booking.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.auth.service.EmailService;
import com._glab.booking_system.booking.exception.BookingNotAuthorizedException;
import com._glab.booking_system.booking.exception.ReservationNotFoundException;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.booking.response.ReservationResponse;
import com._glab.booking_system.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationManagementService {

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final LabManagerAuthorizationService authorizationService;
    private final EmailService emailService;

    /**
     * Get pending reservations for a lab manager or admin.
     * For lab managers: returns only reservations for labs they manage.
     * For admins: returns all pending reservations.
     */
    public List<ReservationResponse> getPendingReservationsForManager(User user) {
        log.debug("Fetching pending reservations for manager: {}", user.getEmail());
        List<Reservation> reservations = authorizationService.getPendingReservationsForUser(user);
        List<ReservationResponse> responses = reservations.stream()
                .map(reservation -> reservationService.toReservationResponse(reservation))
                .collect(Collectors.toList());
        log.debug("Found {} pending reservations for manager {}", responses.size(), user.getEmail());
        return responses;
    }

    /**
     * Approve a single reservation.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @Transactional
    public void approveReservation(UUID reservationId, User manager, String reason) {
        log.info("Manager {} attempting to approve reservation {}", manager.getEmail(), reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found: {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        // Check authorization
        if (!authorizationService.canManageReservation(manager, reservation)) {
            log.warn("User {} is not authorized to manage reservation {}", manager.getEmail(), reservationId);
            throw new BookingNotAuthorizedException("You are not authorized to manage this reservation");
        }

        // Only approve if status is PENDING
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            log.warn("Cannot approve reservation {} with status {}", reservationId, reservation.getStatus());
            throw new IllegalStateException("Only PENDING reservations can be approved");
        }

        reservation.setStatus(ReservationStatus.APPROVED);
        reservationRepository.save(reservation);

        log.info("Reservation {} approved by manager {}", reservationId, manager.getEmail());

        // Send email notification
        sendStatusChangeEmail(reservation, ReservationStatus.APPROVED, reason);
    }

    /**
     * Decline/reject a single reservation.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @Transactional
    public void declineReservation(UUID reservationId, User manager, String reason) {
        log.info("Manager {} attempting to decline reservation {}", manager.getEmail(), reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> {
                    log.warn("Reservation not found: {}", reservationId);
                    return new ReservationNotFoundException(reservationId);
                });

        // Check authorization
        if (!authorizationService.canManageReservation(manager, reservation)) {
            log.warn("User {} is not authorized to manage reservation {}", manager.getEmail(), reservationId);
            throw new BookingNotAuthorizedException("You are not authorized to manage this reservation");
        }

        // Only decline if status is PENDING
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            log.warn("Cannot decline reservation {} with status {}", reservationId, reservation.getStatus());
            throw new IllegalStateException("Only PENDING reservations can be declined");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        reservationRepository.save(reservation);

        log.info("Reservation {} declined by manager {}", reservationId, manager.getEmail());

        // Send email notification
        sendStatusChangeEmail(reservation, ReservationStatus.REJECTED, reason);
    }

    /**
     * Approve all reservations in a recurring group.
     * Requires: User must be a lab manager for the reservations' lab or an admin.
     */
    @Transactional
    public void approveRecurringGroup(UUID recurringGroupId, User manager, String reason) {
        log.info("Manager {} attempting to approve recurring group {}", manager.getEmail(), recurringGroupId);
        
        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            log.warn("No reservations found for recurring group {}", recurringGroupId);
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization for all reservations (they should all be in the same lab)
        Reservation firstReservation = reservations.get(0);
        if (!authorizationService.canManageReservation(manager, firstReservation)) {
            log.warn("User {} is not authorized to manage recurring group {}", manager.getEmail(), recurringGroupId);
            throw new BookingNotAuthorizedException("You are not authorized to manage this recurring group");
        }

        // Approve all PENDING reservations in the group
        int approvedCount = 0;
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                reservation.setStatus(ReservationStatus.APPROVED);
                reservationRepository.save(reservation);
                approvedCount++;
                
                // Send email notification for each approved reservation
                sendStatusChangeEmail(reservation, ReservationStatus.APPROVED, reason);
            }
        }

        log.info("Approved {} reservations in recurring group {} by manager {}", 
                approvedCount, recurringGroupId, manager.getEmail());
    }

    /**
     * Decline all reservations in a recurring group.
     * Requires: User must be a lab manager for the reservations' lab or an admin.
     */
    @Transactional
    public void declineRecurringGroup(UUID recurringGroupId, User manager, String reason) {
        log.info("Manager {} attempting to decline recurring group {}", manager.getEmail(), recurringGroupId);
        
        List<Reservation> reservations = reservationRepository.findByRecurringGroupId(recurringGroupId);
        
        if (reservations.isEmpty()) {
            log.warn("No reservations found for recurring group {}", recurringGroupId);
            throw new ReservationNotFoundException("No reservations found for recurring group: " + recurringGroupId);
        }

        // Check authorization for all reservations (they should all be in the same lab)
        Reservation firstReservation = reservations.get(0);
        if (!authorizationService.canManageReservation(manager, firstReservation)) {
            log.warn("User {} is not authorized to manage recurring group {}", manager.getEmail(), recurringGroupId);
            throw new BookingNotAuthorizedException("You are not authorized to manage this recurring group");
        }

        // Decline all PENDING reservations in the group
        int declinedCount = 0;
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                reservation.setStatus(ReservationStatus.REJECTED);
                reservationRepository.save(reservation);
                declinedCount++;
                
                // Send email notification for each declined reservation
                sendStatusChangeEmail(reservation, ReservationStatus.REJECTED, reason);
            }
        }

        log.info("Declined {} reservations in recurring group {} by manager {}", 
                declinedCount, recurringGroupId, manager.getEmail());
    }

    /**
     * Send email notification when reservation status changes.
     */
    private void sendStatusChangeEmail(Reservation reservation, ReservationStatus newStatus, String reason) {
        try {
            User requester = reservation.getUser();
            String userName = requester.getFirstName() + " " + requester.getLastName();
            String labName = reservation.getLab().getName();
            String startTime = reservation.getStartTime().toString();
            String endTime = reservation.getEndTime().toString();
            String statusString = newStatus.name();

            emailService.sendReservationStatusChangeEmail(
                    requester.getEmail(),
                    userName,
                    labName,
                    startTime,
                    endTime,
                    statusString,
                    reason
            );

            log.debug("Status change email sent for reservation {} to {}", reservation.getId(), requester.getEmail());
        } catch (Exception e) {
            // Log but don't fail the operation if email fails
            log.error("Failed to send status change email for reservation {}: {}", 
                    reservation.getId(), e.getMessage());
        }
    }
}
