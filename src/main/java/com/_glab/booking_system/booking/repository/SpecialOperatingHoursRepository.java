package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.SpecialOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpecialOperatingHoursRepository extends JpaRepository<SpecialOperatingHours, Integer> {

    List<SpecialOperatingHours> findByLabId(Integer labId);

    List<SpecialOperatingHours> findByLabIdAndSpecificDateBetween(Integer labId, LocalDate start, LocalDate end);

    Optional<SpecialOperatingHours> findByLabIdAndSpecificDate(Integer labId, LocalDate date);

    List<SpecialOperatingHours> findByBuildingId(Integer buildingId);

    Optional<SpecialOperatingHours> findByBuildingIdAndSpecificDate(Integer buildingId, LocalDate date);
}
