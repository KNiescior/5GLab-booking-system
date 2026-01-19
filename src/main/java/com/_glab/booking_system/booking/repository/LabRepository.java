package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Building;
import com._glab.booking_system.booking.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabRepository extends JpaRepository<Lab, Integer> {

    List<Lab> findByBuilding(Building building);

    List<Lab> findByBuildingId(Integer buildingId);

    Optional<Lab> findByBuildingAndName(Building building, String name);
}
