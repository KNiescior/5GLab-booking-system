package com._glab.booking_system.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.BuildingClosedDay;
import com._glab.booking_system.booking.model.BuildingOperatingHours;
import com._glab.booking_system.booking.repository.BuildingClosedDayRepository;
import com._glab.booking_system.booking.repository.BuildingOperatingHoursRepository;
import com._glab.booking_system.booking.repository.BuildingRepository;
import com._glab.booking_system.booking.request.DaysOffRequest;
import com._glab.booking_system.booking.request.OperatingHoursRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final BuildingOperatingHoursRepository buildingOperatingHoursRepository;
    private final BuildingClosedDayRepository buildingClosedDayRepository;

    public List<Building> getBuildings() {
        log.debug("Retrieving all active buildings from database");
        List<Building> buildings = buildingRepository.findAllByActiveTrue();
        log.debug("Retrieved {} buildings", buildings.size());
        return buildings;
    }

    public Building getBuildingById(Integer id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new com._glab.booking_system.booking.exception.BuildingNotFoundException(id));
    }

    public Building getActiveBuildingById(Integer id) {
        return buildingRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new com._glab.booking_system.booking.exception.BuildingNotFoundException(id));
    }

    public List<Building> getAllBuildingsForAdmin() {
        return buildingRepository.findAll();
    }

    public Building createBuilding(com._glab.booking_system.booking.request.CreateBuildingRequest request) {
        Building building = new Building();
        building.setName(request.getName());
        building.setDescription(request.getDescription());
        building.setAddress(request.getAddress());
        building.setCity(request.getCity());
        building.setActive(true);
        return buildingRepository.save(building);
    }

    public Building updateBuilding(Integer id, com._glab.booking_system.booking.request.UpdateBuildingRequest request) {
        Building building = getBuildingById(id);
        if (Boolean.FALSE.equals(building.getActive())) {
            throw new IllegalStateException("Cannot update archived building");
        }
        if (request.getName() != null) building.setName(request.getName());
        if (request.getDescription() != null) building.setDescription(request.getDescription());
        if (request.getAddress() != null) building.setAddress(request.getAddress());
        if (request.getCity() != null) building.setCity(request.getCity());
        return buildingRepository.save(building);
    }

    public Building archiveBuilding(Integer id) {
        Building building = getBuildingById(id);
        building.setActive(false);
        building.setArchivedAt(java.time.OffsetDateTime.now());
        return buildingRepository.save(building);
    }

    public void hardDeleteBuilding(Integer id) {
        Building building = getBuildingById(id);
        buildingRepository.delete(building);
        log.info("Hard deleted building id={}", id);
    }

    public List<BuildingOperatingHours> getBuildingOperatingHours(Integer buildingId) {
        getBuildingById(buildingId);
        return buildingOperatingHoursRepository.findByBuildingId(buildingId);
    }

    @Transactional
    public BuildingOperatingHours setBuildingOperatingHours(Integer buildingId, OperatingHoursRequest request) {
        Building building = getBuildingById(buildingId);
        BuildingOperatingHours hours = buildingOperatingHoursRepository
                .findByBuildingIdAndDayOfWeek(buildingId, request.getDayOfWeek())
                .orElseGet(() -> {
                    BuildingOperatingHours h = new BuildingOperatingHours();
                    h.setBuilding(building);
                    h.setDayOfWeek(request.getDayOfWeek());
                    return h;
                });
        hours.setOpenTime(request.getOpenTime());
        hours.setCloseTime(request.getCloseTime());
        hours.setIsClosed(request.getIsClosed() != null ? request.getIsClosed() : false);
        return buildingOperatingHoursRepository.save(hours);
    }

    @Transactional
    public void deleteBuildingOperatingHours(Integer buildingId, Integer dayOfWeek) {
        getBuildingById(buildingId);
        buildingOperatingHoursRepository.deleteByBuildingIdAndDayOfWeek(buildingId, dayOfWeek);
    }

    public List<BuildingClosedDay> getBuildingDaysOff(Integer buildingId) {
        getBuildingById(buildingId);
        return buildingClosedDayRepository.findByBuildingId(buildingId);
    }

    @Transactional
    public BuildingClosedDay addBuildingDayOff(Integer buildingId, DaysOffRequest request) {
        Building building = getBuildingById(buildingId);
        if (request.getSpecificDate() == null && request.getRecurringDayOfWeek() == null) {
            throw new IllegalArgumentException("Either specificDate or recurringDayOfWeek must be set");
        }
        BuildingClosedDay day = new BuildingClosedDay();
        day.setBuilding(building);
        day.setSpecificDate(request.getSpecificDate());
        day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        day.setReason(request.getReason());
        return buildingClosedDayRepository.save(day);
    }

    @Transactional
    public BuildingClosedDay updateBuildingDayOff(Integer buildingId, Integer dayOffId, DaysOffRequest request) {
        getBuildingById(buildingId);
        BuildingClosedDay day = buildingClosedDayRepository.findById(dayOffId)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found"));
        if (!day.getBuilding().getId().equals(buildingId)) {
            throw new IllegalArgumentException("Day off does not belong to this building");
        }
        if (request.getSpecificDate() != null) day.setSpecificDate(request.getSpecificDate());
        if (request.getRecurringDayOfWeek() != null) day.setRecurringDayOfWeek(request.getRecurringDayOfWeek());
        if (request.getReason() != null) day.setReason(request.getReason());
        return buildingClosedDayRepository.save(day);
    }

    @Transactional
    public void deleteBuildingDayOff(Integer buildingId, Integer dayOffId) {
        getBuildingById(buildingId);
        BuildingClosedDay day = buildingClosedDayRepository.findById(dayOffId)
                .orElseThrow(() -> new IllegalArgumentException("Day off not found"));
        if (!day.getBuilding().getId().equals(buildingId)) {
            throw new IllegalArgumentException("Day off does not belong to this building");
        }
        buildingClosedDayRepository.delete(day);
    }
}

