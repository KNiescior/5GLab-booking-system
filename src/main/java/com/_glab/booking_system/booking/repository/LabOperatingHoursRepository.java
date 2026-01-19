package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabOperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabOperatingHoursRepository extends JpaRepository<LabOperatingHours, Integer> {

    List<LabOperatingHours> findByLab(Lab lab);

    List<LabOperatingHours> findByLabId(Integer labId);

    Optional<LabOperatingHours> findByLabAndDayOfWeek(Lab lab, Integer dayOfWeek);

    Optional<LabOperatingHours> findByLabIdAndDayOfWeek(Integer labId, Integer dayOfWeek);
}
