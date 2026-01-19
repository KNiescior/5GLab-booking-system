package com._glab.booking_system.booking.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.response.CurrentAvailabilityResponse;
import com._glab.booking_system.booking.response.LabAvailabilityResponse;
import com._glab.booking_system.booking.response.LabWorkstationsResponse;
import com._glab.booking_system.booking.service.AvailabilityService;
import com._glab.booking_system.booking.service.LabService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
@Slf4j
public class LabController {

    private final LabService labService;
    private final AvailabilityService availabilityService;

    /**
     * GET /api/v1/labs/{labId} - Get lab details
     */
    @GetMapping("/{labId}")
    public ResponseEntity<Lab> getLabById(@PathVariable Integer labId) {
        return labService.getLabById(labId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new LabNotFoundException(labId));
    }

    /**
     * GET /api/v1/labs/{labId}/availability?week=2026-01-19 - Get weekly availability
     */
    @GetMapping("/{labId}/availability")
    public ResponseEntity<LabAvailabilityResponse> getWeeklyAvailability(
            @PathVariable Integer labId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week) {
        
        log.info("Getting weekly availability for lab {} starting week {}", labId, week);
        LabAvailabilityResponse response = availabilityService.getWeeklyAvailability(labId, week);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/labs/{labId}/availability/current - Get current availability
     */
    @GetMapping("/{labId}/availability/current")
    public ResponseEntity<CurrentAvailabilityResponse> getCurrentAvailability(@PathVariable Integer labId) {
        log.info("Getting current availability for lab {}", labId);
        CurrentAvailabilityResponse response = availabilityService.getCurrentAvailability(labId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/labs/{labId}/workstations - Get all workstations for a lab
     */
    @GetMapping("/{labId}/workstations")
    public ResponseEntity<LabWorkstationsResponse> getLabWorkstations(@PathVariable Integer labId) {
        log.info("Getting workstations for lab {}", labId);
        LabWorkstationsResponse response = availabilityService.getLabWorkstations(labId);
        return ResponseEntity.ok(response);
    }
}
