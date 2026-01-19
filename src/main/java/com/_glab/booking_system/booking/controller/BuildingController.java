package com._glab.booking_system.booking.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.service.BuildingService;
import com._glab.booking_system.booking.service.LabService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/buildings")
@RequiredArgsConstructor
@Slf4j
public class BuildingController {

    private final BuildingService buildingService;
    private final LabService labService;

    /**
     * GET /api/v1/buildings - List all buildings
     */
    @GetMapping
    public ResponseEntity<List<Building>> getBuildings() {
        log.debug("Fetching all buildings");
        List<Building> buildings = buildingService.getBuildings();
        log.debug("Found {} buildings", buildings.size());
        return ResponseEntity.ok(buildings);
    }

    /**
     * GET /api/v1/buildings/{buildingId}/labs - List labs in a building
     */
    @GetMapping("/{buildingId}/labs")
    public ResponseEntity<List<Lab>> getLabsByBuilding(@PathVariable Integer buildingId) {
        log.debug("Fetching labs for building {}", buildingId);
        List<Lab> labs = labService.getLabsByBuildingId(buildingId);
        log.debug("Found {} labs in building {}", labs.size(), buildingId);
        return ResponseEntity.ok(labs);
    }
}
