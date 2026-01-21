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
import com._glab.booking_system.booking.exception.ReservationNotFoundException;
import com._glab.booking_system.booking.request.CreateReservationRequest;
import com._glab.booking_system.booking.request.EditReservationRequest;
import com._glab.booking_system.booking.request.RejectEditRequest;
import com._glab.booking_system.booking.response.RecurringReservationResponse;
import com._glab.booking_system.booking.response.ReservationResponse;
import com._glab.booking_system.booking.service.ReservationEditService;
import com._glab.booking_system.booking.service.ReservationService;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationEditService reservationEditService;
    private final UserRepository userRepository;

    /**
     * Create a new reservation (single or recurring).
     * If recurring config is provided, creates all occurrences.
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReservationRequest request) {
        
        log.info("Reservation creation request received for lab {} by user {}", 
                request.getLabId(), userDetails.getUsername());
        
        // Note: getUsername() returns the email address in this app
        // (see CustomUserDetailsService which loads users by email)
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        // If recurring, return full recurring response
        if (request.getRecurring() != null) {
            log.info("Creating recurring reservation for user {} in lab {}", user.getEmail(), request.getLabId());
            RecurringReservationResponse response = reservationService.createRecurringReservation(request, user);
            log.info("Recurring reservation created with group ID {} ({} occurrences)", 
                    response.getRecurringGroupId(), response.getTotalOccurrences());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        
        // Single reservation
        ReservationResponse response = reservationService.createReservation(request, user);
        log.info("Single reservation created with ID {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a reservation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        log.debug("Fetching reservation by ID: {}", id);
        return reservationService.getReservationById(id)
                .map(response -> {
                    log.debug("Reservation {} found", id);
                    return ResponseEntity.ok(response);
                })
                .orElseThrow(() -> {
                    log.warn("Reservation not found: {}", id);
                    return new ReservationNotFoundException(id);
                });
    }

    /**
     * Get current user's reservations.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.debug("Fetching reservations for user: {}", userDetails.getUsername());
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> {
                    log.error("Authenticated user not found: {}", userDetails.getUsername());
                    return new AuthenticationFailedException("Authenticated user not found");
                });
        
        List<ReservationResponse> reservations = reservationService.getUserReservations(user.getId());
        log.debug("Found {} reservations for user {}", reservations.size(), user.getUsername());
        return ResponseEntity.ok(reservations);
    }

    /**
     * Professor edits their own reservation.
     * If PENDING: changes applied automatically.
     * If APPROVED: creates edit proposal requiring lab manager re-approval.
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
        
        reservationEditService.editReservationByProfessor(reservationId, request, user);
        
        log.info("Reservation {} edited by professor {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Professor approves lab manager's edit of their reservation.
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
        
        reservationEditService.approveEditByProfessor(reservationId, user);
        
        log.info("Edit approved for reservation {} by professor {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Professor rejects lab manager's edit of their reservation.
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
        reservationEditService.rejectEditByProfessor(reservationId, user, reason);
        
        log.info("Edit rejected for reservation {} by professor {}", reservationId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Professor edits their own recurring group.
     * If all occurrences are PENDING: changes applied automatically.
     * If any occurrence is APPROVED: those need re-approval.
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
        
        reservationEditService.editRecurringGroupByProfessor(recurringGroupId, request, user);
        
        log.info("Recurring group {} edited by professor {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Professor approves lab manager's edit of their recurring group.
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
        
        reservationEditService.approveRecurringGroupEditByProfessor(recurringGroupId, user);
        
        log.info("Edit approved for recurring group {} by professor {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Professor rejects lab manager's edit of their recurring group.
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
        reservationEditService.rejectRecurringGroupEditByProfessor(recurringGroupId, user, reason);
        
        log.info("Edit rejected for recurring group {} by professor {}", recurringGroupId, user.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
