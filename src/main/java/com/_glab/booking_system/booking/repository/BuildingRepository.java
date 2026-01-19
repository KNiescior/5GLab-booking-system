package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Integer> {

    Optional<Building> findByName(String name);

    List<Building> findAll();
}
