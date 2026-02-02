package com._glab.booking_system.booking.controller;

import com._glab.booking_system.booking.exception.WorkstationNotFoundException;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.request.CreateWorkstationRequest;
import com._glab.booking_system.booking.request.UpdateWorkstationRequest;
import com._glab.booking_system.booking.service.WorkstationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin-only controller for workstation CRUD.
 */
@RestController
@RequestMapping("/api/v1/admin/workstations")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminWorkstationController {

    private final WorkstationService workstationService;

    @PostMapping
    public ResponseEntity<Workstation> createWorkstation(@Valid @RequestBody CreateWorkstationRequest request) {
        log.info("Admin creating workstation in lab {}", request.getLabId());
        Workstation ws = workstationService.createWorkstation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ws);
    }

    @GetMapping
    public ResponseEntity<List<Workstation>> listWorkstations(@RequestParam(required = false) Integer labId) {
        if (labId != null) {
            return ResponseEntity.ok(workstationService.getWorkstationsByLabId(labId));
        }
        return ResponseEntity.ok(workstationService.getAllWorkstations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workstation> getWorkstation(@PathVariable Integer id) {
        try {
            Workstation ws = workstationService.getWorkstationById(id);
            return ResponseEntity.ok(ws);
        } catch (WorkstationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workstation> updateWorkstation(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateWorkstationRequest request) {
        log.info("Admin updating workstation id={}", id);
        try {
            Workstation ws = workstationService.updateWorkstation(id, request);
            return ResponseEntity.ok(ws);
        } catch (WorkstationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Workstation> archiveWorkstation(@PathVariable Integer id) {
        log.info("Admin archiving workstation id={}", id);
        try {
            Workstation ws = workstationService.archiveWorkstation(id);
            return ResponseEntity.ok(ws);
        } catch (WorkstationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
