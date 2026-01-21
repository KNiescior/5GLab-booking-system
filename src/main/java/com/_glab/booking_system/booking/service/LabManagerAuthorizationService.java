package com._glab.booking_system.booking.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.repository.LabManagerRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.ReservationRepository;
import com._glab.booking_system.user.model.RoleName;
import com._glab.booking_system.user.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for authorization checks related to lab manager and admin permissions.
 * Admins have full access to all labs, while lab managers only have access to their assigned labs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LabManagerAuthorizationService {

    private final LabManagerRepository labManagerRepository;
    private final LabRepository labRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Check if user has ADMIN role.
     *
     * @param user The user to check
     * @return true if user is an admin
     */
    public boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        boolean isAdmin = user.getRole().getName() == RoleName.ADMIN;
        log.trace("Admin check for user {}: {}", user.getEmail(), isAdmin);
        return isAdmin;
    }

    /**
     * Check if user is a lab manager for a specific lab OR is an admin.
     *
     * @param user  The user to check
     * @param labId The lab ID to check
     * @return true if user is a lab manager for the lab or is an admin
     */
    public boolean isLabManagerForLab(User user, Integer labId) {
        if (user == null || labId == null) {
            return false;
        }

        // Admins can manage all labs
        if (isAdmin(user)) {
            log.trace("User {} is admin, has access to lab {}", user.getEmail(), labId);
            return true;
        }

        // Check if user is a lab manager for this lab
        boolean isManager = labManagerRepository.existsByLabAndUser(
                labRepository.findById(labId).orElse(null),
                user
        );
        log.trace("Lab manager check for user {} and lab {}: {}", user.getEmail(), labId, isManager);
        return isManager;
    }

    /**
     * Check if user is a lab manager for the reservation's lab OR is an admin.
     *
     * @param user        The user to check
     * @param reservation The reservation to check
     * @return true if user is a lab manager for the reservation's lab or is an admin
     */
    public boolean isLabManagerForReservation(User user, Reservation reservation) {
        if (user == null || reservation == null || reservation.getLab() == null) {
            return false;
        }

        return isLabManagerForLab(user, reservation.getLab().getId());
    }

    /**
     * Check if user can manage a reservation (lab manager for that lab OR admin).
     * This is an alias for isLabManagerForReservation for clarity.
     *
     * @param user        The user to check
     * @param reservation The reservation to check
     * @return true if user can manage the reservation
     */
    public boolean canManageReservation(User user, Reservation reservation) {
        return isLabManagerForReservation(user, reservation);
    }

    /**
     * Check if user is the owner (requester) of a reservation.
     *
     * @param user        The user to check
     * @param reservation The reservation to check
     * @return true if user is the owner of the reservation
     */
    public boolean isReservationOwner(User user, Reservation reservation) {
        if (user == null || reservation == null || reservation.getUser() == null) {
            return false;
        }
        boolean isOwner = reservation.getUser().getId().equals(user.getId());
        log.trace("Reservation owner check for user {} and reservation {}: {}", 
                user.getEmail(), reservation.getId(), isOwner);
        return isOwner;
    }

    /**
     * Get list of labs that the user manages.
     * For admins, returns all labs.
     * For lab managers, returns only labs they are assigned to.
     *
     * @param user The user
     * @return List of labs that the user can manage
     */
    public List<Lab> getManagedLabs(User user) {
        if (user == null) {
            return List.of();
        }

        // Admins can manage all labs
        if (isAdmin(user)) {
            log.debug("User {} is admin, returning all labs", user.getEmail());
            return labRepository.findAll();
        }

        // Lab managers: return only labs they manage
        List<Lab> managedLabs = labManagerRepository.findByUser(user).stream()
                .map(com._glab.booking_system.booking.model.LabManager::getLab)
                .collect(Collectors.toList());
        log.debug("User {} manages {} labs", user.getEmail(), managedLabs.size());
        return managedLabs;
    }

    /**
     * Get list of lab IDs that the user manages.
     * For admins, returns all lab IDs.
     * For lab managers, returns only lab IDs they are assigned to.
     *
     * @param user The user
     * @return List of lab IDs that the user can manage
     */
    public List<Integer> getManagedLabIds(User user) {
        return getManagedLabs(user).stream()
                .map(Lab::getId)
                .collect(Collectors.toList());
    }

    /**
     * Get pending reservations for a user.
     * For admins: returns all pending reservations across all labs.
     * For lab managers: returns only pending reservations for labs they manage.
     *
     * @param user The user
     * @return List of pending reservations
     */
    public List<Reservation> getPendingReservationsForUser(User user) {
        if (user == null) {
            return List.of();
        }

        // Admins see all pending reservations
        if (isAdmin(user)) {
            log.debug("User {} is admin, returning all pending reservations", user.getEmail());
            return reservationRepository.findByStatus(ReservationStatus.PENDING);
        }

        // Lab managers see only reservations for their labs
        log.debug("User {} is lab manager, returning pending reservations for managed labs", user.getEmail());
        return reservationRepository.findPendingReservationsForManager(user.getId());
    }
}
