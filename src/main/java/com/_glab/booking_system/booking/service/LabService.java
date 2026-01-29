package com._glab.booking_system.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.exception.BuildingNotFoundException;
import com._glab.booking_system.booking.exception.LabNotFoundException;
import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabManager;
import com._glab.booking_system.booking.model.LabClosedDay;
import com._glab.booking_system.booking.model.LabOperatingHours;
import com._glab.booking_system.booking.model.SpecialOperatingHours;
import com._glab.booking_system.booking.repository.BuildingRepository;
import com._glab.booking_system.booking.repository.LabClosedDayRepository;
import com._glab.booking_system.booking.repository.LabManagerRepository;
import com._glab.booking_system.booking.repository.LabOperatingHoursRepository;
import com._glab.booking_system.booking.repository.LabRepository;
import com._glab.booking_system.booking.repository.SpecialOperatingHoursRepository;
import com._glab.booking_system.booking.request.CreateLabRequest;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;
import com._glab.booking_system.booking.request.SpecialOperatingHoursRequest;
import com._glab.booking_system.booking.request.UpdateLabRequest;
import com._glab.booking_system.user.exception.UserNotFoundException;
import com._glab.booking_system.user.model.User;
import com._glab.booking_system.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabService {

    private final LabRepository labRepository;
    private final BuildingRepository buildingRepository;
    private final LabManagerRepository labManagerRepository;
    private final LabOperatingHoursRepository labOperatingHoursRepository;
    private final LabClosedDayRepository labClosedDayRepository;
    private final SpecialOperatingHoursRepository specialOperatingHoursRepository;
    private final UserRepository userRepository;

    public List<Lab> getLabsByBuildingId(Integer buildingId) {
        log.debug("Retrieving active labs for building {}", buildingId);
        List<Lab> labs = labRepository.findByBuildingIdAndActiveTrue(buildingId);
        log.debug("Found {} labs in building {}", labs.size(), buildingId);
        return labs;
    }

    public Optional<Lab> getLabById(Integer labId) {
        log.debug("Retrieving lab by ID: {}", labId);
        return labRepository.findById(labId);
    }

    public Lab getLabByIdOrThrow(Integer labId) {
        return labRepository.findById(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));
    }

    public Lab getActiveLabByIdOrThrow(Integer labId) {
        return labRepository.findByIdAndActiveTrue(labId)
                .orElseThrow(() -> new LabNotFoundException(labId));
    }

    public List<Lab> getAllLabsForAdmin() {
        return labRepository.findAll();
    }

    @Transactional
    public Lab createLab(CreateLabRequest request) {
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new BuildingNotFoundException(request.getBuildingId()));
        if (Boolean.FALSE.equals(building.getActive())) {
            throw new IllegalStateException("Cannot add lab to archived building");
        }
        Lab lab = new Lab();
        lab.setBuilding(building);
        lab.setName(request.getName());
        lab.setDescription(request.getDescription());
        lab.setCapacity(request.getCapacity());
        lab.setDefaultOpenTime(request.getDefaultOpenTime());
        lab.setDefaultCloseTime(request.getDefaultCloseTime());
        lab.setActive(true);
        return labRepository.save(lab);
    }

    @Transactional
    public Lab updateLab(Integer id, UpdateLabRequest request) {
        Lab lab = getLabByIdOrThrow(id);
        if (Boolean.FALSE.equals(lab.getActive())) {
            throw new IllegalStateException("Cannot update archived lab");
        }
        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new BuildingNotFoundException(request.getBuildingId()));
            lab.setBuilding(building);
        }
        if (request.getName() != null) lab.setName(request.getName());
        if (request.getDescription() != null) lab.setDescription(request.getDescription());
        if (request.getCapacity() != null) lab.setCapacity(request.getCapacity());
        if (request.getDefaultOpenTime() != null) lab.setDefaultOpenTime(request.getDefaultOpenTime());
        if (request.getDefaultCloseTime() != null) lab.setDefaultCloseTime(request.getDefaultCloseTime());
        return labRepository.save(lab);
    }

    @Transactional
    public Lab archiveLab(Integer id) {
        Lab lab = getLabByIdOrThrow(id);
        lab.setActive(false);
        lab.setArchivedAt(OffsetDateTime.now());
        return labRepository.save(lab);
    }

    @Transactional
    public void hardDeleteLab(Integer id) {
        Lab lab = getLabByIdOrThrow(id);
        labManagerRepository.findByLab(lab).forEach(labManagerRepository::delete);
        labRepository.delete(lab);
        log.info("Hard deleted lab id={}", id);
    }

    @Transactional
    public void addLabManager(Integer labId, Integer userId) {
        Lab lab = getLabByIdOrThrow(labId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (labManagerRepository.existsByLabAndUser(lab, user)) {
            throw new IllegalStateException("User is already a lab manager for this lab");
        }
        LabManager lm = new LabManager();
        lm.setLab(lab);
        lm.setUser(user);
        lm.setIsPrimary(false);
        labManagerRepository.save(lm);
        log.info("Added user {} as lab manager for lab {}", user.getEmail(), labId);
    }

    @Transactional
    public void removeLabManager(Integer labId, Integer userId) {
        Lab lab = getLabByIdOrThrow(labId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        labManagerRepository.findByLabAndUser(lab, user).ifPresent(labManagerRepository::delete);
        log.info("Removed user {} as lab manager for lab {}", user.getEmail(), labId);
    }

    public List<LabManager> getLabManagers(Integer labId) {
        Lab lab = getLabByIdOrThrow(labId);
        return labManagerRepository.findByLab(lab);
    }

    public List<LabOperatingHours> getLabOperatingHours(Integer labId) {
        getLabByIdOrThrow(labId);
        return labOperatingHoursRepository.findByLabId(labId);
    }

    @Transactional
    public LabOperatingHours setLabOperatingHours(Integer labId, OperatingHoursRequest request) {
        Lab lab = getLabByIdOrThrow(labId);
        LabOperatingHours hours = labOperatingHoursRepository
                .findByLabIdAndDayOfWeek(labId, request.getDayOfWeek())
                .orElseGet(() -> {
                    LabOperatingHours h = new LabOperatingHours();
                    h.setLab(lab);
                    h.setDayOfWeek(request.getDayOfWeek());
                    return h;
                });
        hours.setOpenTime(request.getOpenTime());
        hours.setCloseTime(request.getCloseTime());
        hours.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
        return labOperatingHoursRepository.save(hours);
    }

    @Transactional
    public void deleteLabOperatingHours(Integer labId, Integer dayOfWeek) {
        getLabByIdOrThrow(labId);
        labOperatingHoursRepository.findByLabIdAndDayOfWeek(labId, dayOfWeek)
                .ifPresent(labOperatingHoursRepository::delete);
    }

    public List<SpecialOperatingHours> getLabSpecialOperatingHours(Integer labId) {
        getLabByIdOrThrow(labId);
        return specialOperatingHoursRepository.findByLabId(labId);
    }

    @Transactional
    public SpecialOperatingHours setLabSpecialOperatingHours(Integer labId, SpecialOperatingHoursRequest request) {
        Lab lab = getLabByIdOrThrow(labId);
        SpecialOperatingHours special = specialOperatingHoursRepository
                .findByLabIdAndSpecificDate(labId, request.getSpecificDate())
                .orElseGet(() -> {
                    SpecialOperatingHours s = new SpecialOperatingHours();
                    s.setLab(lab);
                    s.setSpecificDate(request.getSpecificDate());
                    return s;
                });
        special.setOpenTime(request.getOpenTime());
        special.setCloseTime(request.getCloseTime());
        special.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
        return specialOperatingHoursRepository.save(special);
    }

    @Transactional
    public void deleteLabSpecialOperatingHours(Integer labId, Integer specialHoursId) {
        Lab lab = getLabByIdOrThrow(labId);
        specialOperatingHoursRepository.findById(specialHoursId)
                .filter(s -> s.getLab() != null && s.getLab().getId().equals(labId))
                .ifPresent(specialOperatingHoursRepository::delete);
    }

    public List<LabClosedDay> getLabDaysOff(Integer labId) {
        getLabByIdOrThrow(labId);
        return labClosedDayRepository.findByLabId(labId);
    }

    @Transactional
    public LabClosedDay addLabDayOff(Integer labId, DaysOffRequest request) {
        Lab lab = getLabByIdOrThrow(labId);
        if (request.getSpecificDate() == null && request.getRecurringDayOfWeek() == null) {
            throw new IllegalArgumentException("Either specificDate or recurringDayOfWeek must be set");
        }
        LabClosedDay day = new LabClosedDay();
        day.setLab(lab);
        day.setSpecificDate(request.getSpecificDate());
        day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        day.setReason(request.getReason());
        return labClosedDayRepository.save(day);
    }

    @Transactional
    public LabClosedDay updateLabDayOff(Integer labId, Integer dayOffId, DaysOffRequest request) {
        getLabByIdOrThrow(labId);
        LabClosedDay day = labClosedDayRepository.findById(dayOffId)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found"));
        if (day.getLab() == null || !day.getLab().getId().equals(labId)) {
            throw new IllegalArgumentException("Day off does not belong to this lab");
        }
        if (request.getSpecificDate() != null) day.setSpecificDate(request.getSpecificDate());
        if (request.getRecurringDayOfWeek() != null) day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        if (request.getReason() != null) day.setReason(request.getReason());
        return labClosedDayRepository.save(day);
    }

    @Transactional
    public void deleteLabDayOff(Integer labId, Integer dayOffId) {
        getLabByIdOrThrow(labId);
        LabClosedDay day = labClosedDayRepository.findById(dayOffId)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found"));
        if (day.getLab() == null || !day.getLab().getId().equals(labId)) {
            throw new IllegalArgumentException("Day off does not belong to this lab");
        }
        labClosedDayRepository.delete(day);
    }
}
