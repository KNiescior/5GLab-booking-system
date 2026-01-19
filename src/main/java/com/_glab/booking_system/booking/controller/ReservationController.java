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

import com._glab.booking_system.booking.request.CreateReservationRequest;
import com._glab.booking_system.booking.response.RecurringReservationResponse;
import com._glab.booking_system.booking.response.ReservationResponse;
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
    private final UserRepository userRepository;

    /**
     * Create a new reservation (single or recurring).
     * If recurring config is provided, creates all occurrences.
     */
    @PostMapping
    public ResponseEntity<?> createReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReservationRequest request) {
        
        // Note: getUsername() returns the email address in this app
        // (see CustomUserDetailsService which loads users by email)
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // If recurring, return full recurring response
        if (request.getRecurring() != null) {
            RecurringReservationResponse response = reservationService.createRecurringReservation(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        
        // Single reservation
        ReservationResponse response = reservationService.createReservation(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a reservation by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(@PathVariable UUID id) {
        return reservationService.getReservationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current user's reservations.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<ReservationResponse> reservations = reservationService.getUserReservations(user.getId());
        return ResponseEntity.ok(reservations);
    }
}
