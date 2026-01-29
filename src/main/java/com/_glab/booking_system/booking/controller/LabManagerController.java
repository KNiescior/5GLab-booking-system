package com._glab.booking_system.booking.controller;

import com._glab.booking_system.auth.exception.AuthenticationFailedException;
import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.exception.WorkstationNotFoundException;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.SpecialOperatingHours;
import com._glab.booking_system.booking.model.Workstation;
import com._glab.booking_system.booking.request.CreateWorkstationRequest;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.SpecialOperatingHoursRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;
import com._glab.booking_system.booking.request.UpdateLabRequest;
import com._glab.booking_system.booking.request.UpdateWorkstationRequest;
import com._glab.booking_system.booking.service.LabManagerAuthorizationService;
import com._glab.booking_system.booking.service.LabService;
import com._glab.booking_system.booking.service.WorkstationService;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
 * Controller for lab managers to view and update their managed labs.
 */
@RestController
@RequestMapping("/api/v1/manager/labs")
@RequiredArgsConstructor
@Slf4j
public class LabManagerController {

    private final LabService labService;
    private final LabManagerAuthorizationService labManagerAuthorizationService;
    private final WorkstationService workstationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<Lab>> getManagedLabs(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        List<Lab> labs = labManagerAuthorizationService.getManagedLabs(user);
        return ResponseEntity.ok(labs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lab> getLab(@PathVariable Integer id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            Lab lab = labService.getLabByIdOrThrow(id);
            return ResponseEntity.ok(lab);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lab> updateLab(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateLabRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            UpdateLabRequest limited = UpdateLabRequest.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .capacity(request.getCapacity())
                    .defaultOpenTime(request.getDefaultOpenTime())
                    .defaultCloseTime(request.getDefaultCloseTime())
                    .build();
            Lab lab = labService.updateLab(id, limited);
            return ResponseEntity.ok(lab);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{labId}/workstations")
    public ResponseEntity<Workstation> createWorkstation(
            @PathVariable Integer labId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateWorkstationRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, labId)) {
            return ResponseEntity.notFound().build();
        }
        if (!request.getLabId().equals(labId)) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Workstation ws = workstationService.createWorkstation(request);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ws);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/workstations/{id}")
    public ResponseEntity<Workstation> updateWorkstation(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateWorkstationRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        try {
            Workstation ws = workstationService.getWorkstationById(id);
            if (!labManagerAuthorizationService.isLabManagerForLab(user, ws.getLab().getId())) {
                return ResponseEntity.notFound().build();
            }
            Workstation updated = workstationService.updateWorkstation(id, request);
            return ResponseEntity.ok(updated);
        } catch (WorkstationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/workstations/{id}")
    public ResponseEntity<Workstation> archiveWorkstation(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        try {
            Workstation ws = workstationService.getWorkstationById(id);
            if (!labManagerAuthorizationService.isLabManagerForLab(user, ws.getLab().getId())) {
                return ResponseEntity.notFound().build();
            }
            Workstation archived = workstationService.archiveWorkstation(id);
            return ResponseEntity.ok(archived);
        } catch (WorkstationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/operating-hours")
    public ResponseEntity<List<LabOperatingHours>> getLabOperatingHours(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(labService.getLabOperatingHours(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/operating-hours")
    public ResponseEntity<LabOperatingHours> setLabOperatingHours(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OperatingHoursRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            LabOperatingHours hours = labService.setLabOperatingHours(id, request);
            return ResponseEntity.ok(hours);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/operating-hours/{dayOfWeek}")
    public ResponseEntity<LabOperatingHours> updateLabOperatingHours(
            @PathVariable Integer id,
            @PathVariable Integer dayOfWeek,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OperatingHoursRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            OperatingHoursRequest req = OperatingHoursRequest.builder()
                    .dayOfWeek(dayOfWeek)
                    .openTime(request.getOpenTime())
                    .closeTime(request.getCloseTime())
                    .isClosed(request.getIsClosed())
                    .build();
            LabOperatingHours hours = labService.setLabOperatingHours(id, req);
            return ResponseEntity.ok(hours);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/operating-hours/{dayOfWeek}")
    public ResponseEntity<Void> deleteLabOperatingHours(
            @PathVariable Integer id,
            @PathVariable Integer dayOfWeek,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            labService.deleteLabOperatingHours(id, dayOfWeek);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/special-hours")
    public ResponseEntity<List<SpecialOperatingHours>> getLabSpecialHours(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(labService.getLabSpecialOperatingHours(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/special-hours")
    public ResponseEntity<SpecialOperatingHours> setLabSpecialHours(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpecialOperatingHoursRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            SpecialOperatingHours special = labService.setLabSpecialOperatingHours(id, request);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(special);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/special-hours/{specialHoursId}")
    public ResponseEntity<Void> deleteLabSpecialHours(
            @PathVariable Integer id,
            @PathVariable Integer specialHoursId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            labService.deleteLabSpecialOperatingHours(id, specialHoursId);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/days-off")
    public ResponseEntity<List<LabClosedDay>> getLabDaysOff(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            return ResponseEntity.ok(labService.getLabDaysOff(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/days-off")
    public ResponseEntity<LabClosedDay> addLabDayOff(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DaysOffRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            LabClosedDay day = labService.addLabDayOff(id, request);
            return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(day);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/days-off/{dayOffId}")
    public ResponseEntity<LabClosedDay> updateLabDayOff(
            @PathVariable Integer id,
            @PathVariable Integer dayOffId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody DaysOffRequest request) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            LabClosedDay day = labService.updateLabDayOff(id, dayOffId, request);
            return ResponseEntity.ok(day);
        } catch (LabNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/days-off/{dayOffId}")
    public ResponseEntity<Void> deleteLabDayOff(
            @PathVariable Integer id,
            @PathVariable Integer dayOffId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new AuthenticationFailedException("Authenticated user not found"));
        if (!labManagerAuthorizationService.isLabManagerForLab(user, id)) {
            return ResponseEntity.notFound().build();
        }
        try {
            labService.deleteLabDayOff(id, dayOffId);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
