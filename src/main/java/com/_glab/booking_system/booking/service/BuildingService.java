package com._glab.booking_system.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.repository.BuildingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final BuildingRepository buildingRepository;

    public List<Building> getBuildings() {
        log.debug("Retrieving all buildings from database");
        List<Building> buildings = buildingRepository.findAll();
        log.debug("Retrieved {} buildings", buildings.size());
        return buildings;
    }
}

