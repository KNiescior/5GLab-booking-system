package com._glab.booking_system.booking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.service.LabService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
@Slf4j
public class LabController {

    private final LabService labService;

    /**
     * GET /api/v1/labs/{labId} - Get lab details
     */
    @GetMapping("/{labId}")
    public ResponseEntity<Lab> getLabById(@PathVariable Integer labId) {
        return labService.getLabById(labId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
