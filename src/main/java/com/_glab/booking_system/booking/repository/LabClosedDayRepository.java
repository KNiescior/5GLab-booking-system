package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.LabClosedDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LabClosedDayRepository extends JpaRepository<LabClosedDay, Integer> {

    List<LabClosedDay> findByLab(Lab lab);

    List<LabClosedDay> findByLabId(Integer labId);

    /**
     * Find all closures (specific dates) for a lab within a date range.
     */
    @Query("SELECT lcd FROM LabClosedDay lcd WHERE (lcd.lab.id = :labId OR lcd.lab IS NULL) " +
           "AND lcd.specificDate BETWEEN :startDate AND :endDate")
    List<LabClosedDay> findSpecificClosuresInRange(
            @Param("labId") Integer labId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all recurring closures (by day of week) for a lab.
     */
    @Query("SELECT lcd FROM LabClosedDay lcd WHERE (lcd.lab.id = :labId OR lcd.lab IS NULL) " +
           "AND lcd.recurringDayOfWeek IS NOT NULL")
    List<LabClosedDay> findRecurringClosures(@Param("labId") Integer labId);

    /**
     * Check if a specific date is closed for a lab.
     */
    @Query("SELECT CASE WHEN COUNT(lcd) > 0 THEN true ELSE false END FROM LabClosedDay lcd " +
           "WHERE (lcd.lab.id = :labId OR lcd.lab IS NULL) " +
           "AND (lcd.specificDate = :date OR lcd.recurringDayOfWeek = :dayOfWeek)")
    boolean isLabClosedOnDate(
            @Param("labId") Integer labId,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") Integer dayOfWeek);
}
