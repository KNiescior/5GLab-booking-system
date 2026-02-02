package com._glab.booking_system.booking.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.booking.model.ReservationStatus;
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

/**
 * Admin-only controller for managing all reservations (like lab manager of all labs).
 */
@RestController
@RequestMapping("/api/v1/admin/reservations")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

    private final ReservationManagementService reservationManagementService;
    private final ReservationEditService reservationEditService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Integer labId,
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo) {
        log.debug("Admin listing reservations with filters: status={}, labId={}, userId={}", status, labId, userId);
        List<ReservationResponse> reservations = reservationManagementService.getAllReservationsForAdmin(
                status, labId, userId, dateFrom, dateTo);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        ReservationResponse response = reservationManagementService.getReservation(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveReservation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) ApproveReservationRequest request) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.approveReservation(id, admin, reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<Void> declineReservation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) DeclineReservationRequest request) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        String reason = (request != null) ? request.getReason() : null;
        reservationManagementService.declineReservation(id, admin, reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/edit")
    public ResponseEntity<Void> editReservation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EditReservationRequest request) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        reservationEditService.editReservationByManager(id, request, admin);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/edit/approve")
    public ResponseEntity<Void> approveEdit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        reservationEditService.approveEditByManager(id, admin);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{id}/edit/reject")
    public ResponseEntity<Void> rejectEdit(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody(required = false) RejectEditRequest request) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        String reason = (request != null) ? request.getReason() : null;
        reservationEditService.rejectEditByManager(id, admin, reason);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
