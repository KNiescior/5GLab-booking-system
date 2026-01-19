package com._glab.booking_system.booking.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.repository.LabRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabRepository labRepository;

    public List<Lab> getLabsByBuildingId(Integer buildingId) {
        return labRepository.findByBuildingId(buildingId);
    }

    public Optional<Lab> getLabById(Integer labId) {
        return labRepository.findById(labId);
    }
}
