package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.BuildingOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingOperatingHoursRepository extends JpaRepository<BuildingOperatingHours, Integer> {

    List<BuildingOperatingHours> findByBuilding(Building building);

    List<BuildingOperatingHours> findByBuildingId(Integer buildingId);

    Optional<BuildingOperatingHours> findByBuildingIdAndDayOfWeek(Integer buildingId, Integer dayOfWeek);

    void deleteByBuildingIdAndDayOfWeek(Integer buildingId, Integer dayOfWeek);
}
