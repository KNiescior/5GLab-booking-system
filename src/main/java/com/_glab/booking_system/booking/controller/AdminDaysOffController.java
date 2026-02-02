package com._glab.booking_system.booking.controller;

import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.service.DaysOffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only controller for university-wide days off.
 */
@RestController
@RequestMapping("/api/v1/admin/university/days-off")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminDaysOffController {

    private final DaysOffService daysOffService;

    @GetMapping
    public ResponseEntity<List<LabClosedDay>> getUniversityDaysOff() {
        return ResponseEntity.ok(daysOffService.getUniversityDaysOff());
    }

    @PostMapping
    public ResponseEntity<LabClosedDay> addUniversityDayOff(@Valid @RequestBody DaysOffRequest request) {
        log.info("Admin adding university day off");
        LabClosedDay day = daysOffService.addUniversityDayOff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(day);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabClosedDay> updateUniversityDayOff(
            @PathVariable Integer id,
            @Valid @RequestBody DaysOffRequest request) {
        log.info("Admin updating university day off id={}", id);
        try {
            LabClosedDay day = daysOffService.updateUniversityDayOff(id, request);
            return ResponseEntity.ok(day);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUniversityDayOff(@PathVariable Integer id) {
        log.info("Admin deleting university day off id={}", id);
        try {
            daysOffService.deleteUniversityDayOff(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
