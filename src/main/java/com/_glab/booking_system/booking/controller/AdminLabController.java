package com._glab.booking_system.booking.controller;

import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.model.LabManager;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.SpecialOperatingHours;
import com._glab.booking_system.booking.request.AddLabManagerRequest;
import com._glab.booking_system.booking.request.CreateLabRequest;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;
import com._glab.booking_system.booking.request.SpecialOperatingHoursRequest;
import com._glab.booking_system.booking.request.UpdateLabRequest;
import com._glab.booking_system.booking.response.LabManagerResponse;
import com._glab.booking_system.booking.service.LabService;
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
import java.util.stream.Collectors;

/**
 * Admin-only controller for lab CRUD and lab manager assignment.
 */
@RestController
@RequestMapping("/api/v1/admin/labs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminLabController {

    private final LabService labService;

    @PostMapping
    public ResponseEntity<Lab> createLab(@Valid @RequestBody CreateLabRequest request) {
        log.info("Admin creating lab: {}", request.getName());
        Lab lab = labService.createLab(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(lab);
    }

    @GetMapping
    public ResponseEntity<List<Lab>> listLabs() {
        log.debug("Admin listing all labs");
        return ResponseEntity.ok(labService.getAllLabsForAdmin());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lab> getLab(@PathVariable Integer id) {
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
            @Valid @RequestBody UpdateLabRequest request) {
        log.info("Admin updating lab id={}", id);
        try {
            Lab lab = labService.updateLab(id, request);
            return ResponseEntity.ok(lab);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Lab> archiveLab(@PathVariable Integer id) {
        log.info("Admin archiving lab id={}", id);
        try {
            Lab lab = labService.archiveLab(id);
            return ResponseEntity.ok(lab);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteLab(@PathVariable Integer id) {
        log.info("Admin hard deleting lab id={}", id);
        try {
            labService.hardDeleteLab(id);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/managers")
    public ResponseEntity<List<LabManagerResponse>> getLabManagers(@PathVariable Integer id) {
        try {
            List<LabManager> managers = labService.getLabManagers(id);
            List<LabManagerResponse> response = managers.stream()
                    .map(LabManagerResponse::fromLabManager)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/managers")
    public ResponseEntity<Void> addLabManager(
            @PathVariable Integer id,
            @Valid @RequestBody AddLabManagerRequest request) {
        log.info("Admin adding user {} as lab manager for lab {}", request.getUserId(), id);
        try {
            labService.addLabManager(id, request.getUserId());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/managers/{userId}")
    public ResponseEntity<Void> removeLabManager(@PathVariable Integer id, @PathVariable Integer userId) {
        log.info("Admin removing user {} as lab manager for lab {}", userId, id);
        try {
            labService.removeLabManager(id, userId);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/operating-hours")
    public ResponseEntity<List<LabOperatingHours>> getLabOperatingHours(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(labService.getLabOperatingHours(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/operating-hours")
    public ResponseEntity<LabOperatingHours> setLabOperatingHours(
            @PathVariable Integer id,
            @Valid @RequestBody OperatingHoursRequest request) {
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
            @Valid @RequestBody OperatingHoursRequest request) {
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
            @PathVariable Integer dayOfWeek) {
        try {
            labService.deleteLabOperatingHours(id, dayOfWeek);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/special-hours")
    public ResponseEntity<List<SpecialOperatingHours>> getLabSpecialHours(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(labService.getLabSpecialOperatingHours(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/special-hours")
    public ResponseEntity<SpecialOperatingHours> setLabSpecialHours(
            @PathVariable Integer id,
            @Valid @RequestBody SpecialOperatingHoursRequest request) {
        try {
            SpecialOperatingHours special = labService.setLabSpecialOperatingHours(id, request);
            return ResponseEntity.ok(special);
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/special-hours/{specialHoursId}")
    public ResponseEntity<Void> deleteLabSpecialHours(
            @PathVariable Integer id,
            @PathVariable Integer specialHoursId) {
        try {
            labService.deleteLabSpecialOperatingHours(id, specialHoursId);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/days-off")
    public ResponseEntity<List<LabClosedDay>> getLabDaysOff(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(labService.getLabDaysOff(id));
        } catch (LabNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/days-off")
    public ResponseEntity<LabClosedDay> addLabDayOff(
            @PathVariable Integer id,
            @Valid @RequestBody DaysOffRequest request) {
        try {
            LabClosedDay day = labService.addLabDayOff(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(day);
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
            @Valid @RequestBody DaysOffRequest request) {
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
            @PathVariable Integer dayOffId) {
        try {
            labService.deleteLabDayOff(id, dayOffId);
            return ResponseEntity.noContent().build();
        } catch (LabNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
