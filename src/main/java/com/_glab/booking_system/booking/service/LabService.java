package com._glab.booking_system.booking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.repository.LabRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabService {

    private final LabRepository labRepository;

    public List<Lab> getLabsByBuildingId(Integer buildingId) {
        log.debug("Retrieving labs for building {}", buildingId);
        List<Lab> labs = labRepository.findByBuildingId(buildingId);
        log.debug("Found {} labs in building {}", labs.size(), buildingId);
        return labs;
    }

    public Optional<Lab> getLabById(Integer labId) {
        log.debug("Retrieving lab by ID: {}", labId);
        Optional<Lab> lab = labRepository.findById(labId);
        if (lab.isPresent()) {
            log.debug("Lab {} found: {}", labId, lab.get().getName());
        } else {
            log.debug("Lab {} not found", labId);
        }
        return lab;
    }
}
