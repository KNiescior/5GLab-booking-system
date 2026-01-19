package com._glab.booking_system.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.repository.BuildingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BuildingService {

    private final BuildingRepository buildingRepository;

    public List<Building> getBuildings() {
        return buildingRepository.findAll();
    }
}

