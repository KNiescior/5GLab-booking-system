package com._glab.booking_system.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.request.DaysOffRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for university-wide (global) days off management.
 * Uses LabClosedDay with lab=null for global closures.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DaysOffService {

    private final LabClosedDayRepository labClosedDayRepository;

    public List<LabClosedDay> getUniversityDaysOff() {
        return labClosedDayRepository.findByLabIsNull();
    }

    @Transactional
    public LabClosedDay addUniversityDayOff(DaysOffRequest request) {
        if (request.getSpecificDate() == null && request.getRecurringDayOfWeek() == null) {
            throw new IllegalArgumentException("Either specificDate or recurringDayOfWeek must be set");
        }
        LabClosedDay day = new LabClosedDay();
        day.setLab(null);
        day.setSpecificDate(request.getSpecificDate());
        day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        day.setReason(request.getReason());
        return labClosedDayRepository.save(day);
    }

    @Transactional
    public LabClosedDay updateUniversityDayOff(Integer id, DaysOffRequest request) {
        LabClosedDay day = labClosedDayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found: " + id));
        if (day.getLab() != null) {
            throw new IllegalArgumentException("Not a university day off");
        }
        if (request.getSpecificDate() != null) day.setSpecificDate(request.getSpecificDate());
        if (request.getRecurringDayOfWeek() != null) day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        if (request.getReason() != null) day.setReason(request.getReason());
        return labClosedDayRepository.save(day);
    }

    @Transactional
    public void deleteUniversityDayOff(Integer id) {
        LabClosedDay day = labClosedDayRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found: " + id));
        if (day.getLab() != null) {
            throw new IllegalArgumentException("Not a university day off");
        }
        labClosedDayRepository.delete(day);
    }
}
