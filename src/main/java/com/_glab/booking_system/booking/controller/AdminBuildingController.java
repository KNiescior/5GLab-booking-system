package com._glab.booking_system.booking.controller;

import com._glab.booking_system.booking.exception.BuildingNotFoundException;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.BuildingClosedDay;
import com._glab.booking_system.booking.model.BuildingOperatingHours;
import com._glab.booking_system.booking.request.CreateBuildingRequest;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;
import com._glab.booking_system.booking.request.UpdateBuildingRequest;
import com._glab.booking_system.booking.service.BuildingService;
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
 * Admin-only controller for building CRUD (create, read, update, archive, hard delete).
 */
@RestController
@RequestMapping("/api/v1/admin/buildings")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminBuildingController {

    private final BuildingService buildingService;

    @PostMapping
    public ResponseEntity<Building> createBuilding(@Valid @RequestBody CreateBuildingRequest request) {
        log.info("Admin creating building: {}", request.getName());
        Building building = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(building);
    }

    @GetMapping
    public ResponseEntity<List<Building>> listBuildings() {
        log.debug("Admin listing all buildings");
        List<Building> buildings = buildingService.getAllBuildingsForAdmin();
        return ResponseEntity.ok(buildings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuilding(@PathVariable Integer id) {
        try {
            Building building = buildingService.getBuildingById(id);
            return ResponseEntity.ok(building);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Building> updateBuilding(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateBuildingRequest request) {
        log.info("Admin updating building id={}", id);
        try {
            Building building = buildingService.updateBuilding(id, request);
            return ResponseEntity.ok(building);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Building> archiveBuilding(@PathVariable Integer id) {
        log.info("Admin archiving building id={}", id);
        try {
            Building building = buildingService.archiveBuilding(id);
            return ResponseEntity.ok(building);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<Void> hardDeleteBuilding(@PathVariable Integer id) {
        log.info("Admin hard deleting building id={}", id);
        try {
            buildingService.hardDeleteBuilding(id);
            return ResponseEntity.noContent().build();
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/operating-hours")
    public ResponseEntity<List<BuildingOperatingHours>> getBuildingOperatingHours(@PathVariable Integer id) {
        try {
            List<BuildingOperatingHours> hours = buildingService.getBuildingOperatingHours(id);
            return ResponseEntity.ok(hours);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/operating-hours")
    public ResponseEntity<BuildingOperatingHours> setBuildingOperatingHours(
            @PathVariable Integer id,
            @Valid @RequestBody OperatingHoursRequest request) {
        try {
            BuildingOperatingHours hours = buildingService.setBuildingOperatingHours(id, request);
            return ResponseEntity.ok(hours);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/operating-hours/{dayOfWeek}")
    public ResponseEntity<BuildingOperatingHours> updateBuildingOperatingHours(
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
            BuildingOperatingHours hours = buildingService.setBuildingOperatingHours(id, req);
            return ResponseEntity.ok(hours);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/operating-hours/{dayOfWeek}")
    public ResponseEntity<Void> deleteBuildingOperatingHours(
            @PathVariable Integer id,
            @PathVariable Integer dayOfWeek) {
        try {
            buildingService.deleteBuildingOperatingHours(id, dayOfWeek);
            return ResponseEntity.noContent().build();
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/days-off")
    public ResponseEntity<List<BuildingClosedDay>> getBuildingDaysOff(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(buildingService.getBuildingDaysOff(id));
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/days-off")
    public ResponseEntity<BuildingClosedDay> addBuildingDayOff(
            @PathVariable Integer id,
            @Valid @RequestBody DaysOffRequest request) {
        try {
            BuildingClosedDay day = buildingService.addBuildingDayOff(id, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(day);
        } catch (BuildingNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/days-off/{dayOffId}")
    public ResponseEntity<BuildingClosedDay> updateBuildingDayOff(
            @PathVariable Integer id,
            @PathVariable Integer dayOffId,
            @Valid @RequestBody DaysOffRequest request) {
        try {
            BuildingClosedDay day = buildingService.updateBuildingDayOff(id, dayOffId, request);
            return ResponseEntity.ok(day);
        } catch (BuildingNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/days-off/{dayOffId}")
    public ResponseEntity<Void> deleteBuildingDayOff(
            @PathVariable Integer id,
            @PathVariable Integer dayOffId) {
        try {
            buildingService.deleteBuildingDayOff(id, dayOffId);
            return ResponseEntity.noContent().build();
        } catch (BuildingNotFoundException | IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
