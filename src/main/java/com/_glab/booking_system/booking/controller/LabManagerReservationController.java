package com._glab.booking_system.booking.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.booking.request.ApproveReservationRequest;
import com._glab.booking_system.booking.request.DeclineReservationRequest;
import com._glab.booking_system.booking.request.EditReservationRequest;
import com._glab.booking_system.booking.request.RejectEditRequest;
import com._glab.booking_system.booking.response.ReservationResponse;
import com._glab.booking_system.booking.service.ReservationEditService;
import com._glab.booking_system.booking.service.ReservationManagementService;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/manager/reservations")
@RequiredArgsConstructor
@Slf4j
public class LabManagerReservationController {
    
    private final ReservationManagementService reservationManagementService;
    private final ReservationEditService reservationEditService;
    private final UserRepository userRepository;
    
    @GetMapping("/pending")
    public ResponseEntity<List<ReservationResponse>> getPendingReservations(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("Fetching pending reservations for manager: {}", userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        List<ReservationResponse> reservations = reservationManagementService.getPendingReservationsForManager(user);
        log.debug("Found {} pending reservations for manager {}", reservations.size(), user.getEmail());
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        log.debug("Fetching reservation by ID: {}", id);
        ReservationResponse response = reservationManagementService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve a single reservation.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveReservation(
            @PathVariable("id") UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) ApproveReservationRequest request) {
        
        log.info("Approval request received for reservation {} by user {}", 
                reservationId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.approveReservation(reservationId, user, reason);
        
        log.info("Reservation {} approved successfully by manager {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Decline/reject a single reservation.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineReservation(
            @PathVariable("id") UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) DeclineReservationRequest request) {
        
        log.info("Decline request received for reservation {} by user {}", 
                reservationId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.declineReservation(reservationId, user, reason);
        
        log.info("Reservation {} declined successfully by manager {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin edits a reservation.
     * Creates an edit proposal requiring professor approval.
     */
    @PostMapping("/{id}/edit")
    public ResponseEntity<Void> editReservation(
            @PathVariable("id") UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EditReservationRequest request) {
        
        log.info("Edit request received for reservation {} by user {}", 
                reservationId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        reservationEditService.editReservationByManager(reservationId, request, user);
        
        log.info("Edit proposal created for reservation {} by manager {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin approves professor's edit of a reservation.
     */
    @PostMapping("/{id}/edit/approve")
    public ResponseEntity<Void> approveEdit(
            @PathVariable("id") UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Edit approval request received for reservation {} by user {}", 
                reservationId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        reservationEditService.approveEditByManager(reservationId, user);
        
        log.info("Edit approved for reservation {} by manager {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin rejects professor's edit of a reservation.
     */
    @PostMapping("/{id}/edit/reject")
    public ResponseEntity<Void> rejectEdit(
            @PathVariable("id") UUID reservationId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) RejectEditRequest request) {
        
        log.info("Edit rejection request received for reservation {} by user {}", 
                reservationId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationEditService.rejectEditByManager(reservationId, user, reason);
        
        log.info("Edit rejected for reservation {} by manager {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Approve all reservations in a recurring group.
     * Requires: User must be a lab manager for the reservations' lab or an admin.
     */
    @PostMapping("/recurring/{groupId}/approve")
    public ResponseEntity<Void> approveRecurringGroup(
            @PathVariable("groupId") UUID recurringGroupId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) ApproveReservationRequest request) {
        
        log.info("Recurring group approval request received for group {} by user {}", 
                recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.approveRecurringGroup(recurringGroupId, user, reason);
        
        log.info("Recurring group {} approved successfully by manager {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Decline all reservations in a recurring group.
     * Requires: User must be a lab manager for the reservations' lab or an admin.
     */
    @PostMapping("/recurring/{groupId}/decline")
    public ResponseEntity<Void> declineRecurringGroup(
            @PathVariable("groupId") UUID recurringGroupId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) DeclineReservationRequest request) {
        
        log.info("Recurring group decline request received for group {} by user {}", 
                recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.declineRecurringGroup(recurringGroupId, user, reason);
        
        log.info("Recurring group {} declined successfully by manager {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin edits all occurrences in a recurring group.
     */
    @PostMapping("/recurring/{groupId}/edit")
    public ResponseEntity<Void> editRecurringGroup(
            @PathVariable("groupId") UUID recurringGroupId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EditReservationRequest request) {
        
        log.info("Recurring group edit request received for group {} by user {}", 
                recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        reservationEditService.editRecurringGroupByManager(recurringGroupId, request, user);
        
        log.info("Edit proposals created for recurring group {} by manager {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin approves professor's edit of a recurring group.
     */
    @PostMapping("/recurring/{groupId}/edit/approve")
    public ResponseEntity<Void> approveRecurringGroupEdit(
            @PathVariable("groupId") UUID recurringGroupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Recurring group edit approval request received for group {} by user {}", 
                recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        reservationEditService.approveRecurringGroupEditByManager(recurringGroupId, user);
        
        log.info("Edit approved for recurring group {} by manager {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin rejects professor's edit of a recurring group.
     */
    @PostMapping("/recurring/{groupId}/edit/reject")
    public ResponseEntity<Void> rejectRecurringGroupEdit(
            @PathVariable("groupId") UUID recurringGroupId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) RejectEditRequest request) {
        
        log.info("Recurring group edit rejection request received for group {} by user {}", 
                recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationEditService.rejectRecurringGroupEditByManager(recurringGroupId, user, reason);
        
        log.info("Edit rejected for recurring group {} by manager {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Approve a single occurrence within a recurring group.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @PostMapping("/recurring/{groupId}/occurrences/{id}/approve")
    public ResponseEntity<Void> approveOccurrence(
            @PathVariable("groupId") UUID recurringGroupId,
            @PathVariable("id") UUID occurrenceId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) ApproveReservationRequest request) {
        
        log.info("Occurrence approval request received for occurrence {} in group {} by user {}", 
                occurrenceId, recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.approveReservation(occurrenceId, user, reason);
        
        log.info("Occurrence {} approved successfully by manager {}", occurrenceId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Decline a single occurrence within a recurring group.
     * Requires: User must be a lab manager for the reservation's lab or an admin.
     */
    @PostMapping("/recurring/{groupId}/occurrences/{id}/decline")
    public ResponseEntity<Void> declineOccurrence(
            @PathVariable("groupId") UUID recurringGroupId,
            @PathVariable("id") UUID occurrenceId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) DeclineReservationRequest request) {
        
        log.info("Occurrence decline request received for occurrence {} in group {} by user {}", 
                occurrenceId, recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.declineReservation(occurrenceId, user, reason);
        
        log.info("Occurrence {} declined successfully by manager {}", occurrenceId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Lab manager/admin edits a single occurrence within a recurring group.
     */
    @PostMapping("/recurring/{groupId}/occurrences/{id}/edit")
    public ResponseEntity<Void> editOccurrence(
            @PathVariable("groupId") UUID recurringGroupId,
            @PathVariable("id") UUID occurrenceId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EditReservationRequest request) {
        
        log.info("Occurrence edit request received for occurrence {} in group {} by user {}", 
                occurrenceId, recurringGroupId, userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        reservationEditService.editRecurringGroupOccurrenceByManager(occurrenceId, request, user);
        
        log.info("Edit proposal created for occurrence {} by manager {}", occurrenceId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
