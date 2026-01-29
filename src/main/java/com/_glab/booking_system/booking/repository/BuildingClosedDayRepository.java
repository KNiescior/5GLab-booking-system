package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.BuildingClosedDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BuildingClosedDayRepository extends JpaRepository<BuildingClosedDay, Integer> {

    List<BuildingClosedDay> findByBuildingId(Integer buildingId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BuildingClosedDay b " +
            "WHERE b.building.id = :buildingId AND (b.specificDate = :date OR b.recurringDayOfWeek = :dayOfWeek)")
    boolean isBuildingClosedOnDate(
            @Param("buildingId") Integer buildingId,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") Integer dayOfWeek);
}
