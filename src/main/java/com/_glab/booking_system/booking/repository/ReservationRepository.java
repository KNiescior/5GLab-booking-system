package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Lab;
import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByUserId(Integer userId);

    List<Reservation> findByLab(Lab lab);

    List<Reservation> findByLabId(Integer labId);

    /**
     * Find all reservations for a lab within a time range.
     */
    @Query("SELECT r FROM Reservation r WHERE r.lab.id = :labId " +
           "AND r.startTime < :endTime AND r.endTime > :startTime " +
           "ORDER BY r.startTime")
    List<Reservation> findByLabIdAndTimeRange(
            @Param("labId") Integer labId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime);

    /**
     * Find reservations for a lab within a time range filtered by status.
     */
    @Query("SELECT r FROM Reservation r WHERE r.lab.id = :labId " +
           "AND r.startTime < :endTime AND r.endTime > :startTime " +
           "AND r.status IN :statuses " +
           "ORDER BY r.startTime")
    List<Reservation> findByLabIdAndTimeRangeAndStatusIn(
            @Param("labId") Integer labId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("statuses") List<ReservationStatus> statuses);

    /**
     * Find current reservations (happening right now) for a lab.
     */
    @Query("SELECT r FROM Reservation r WHERE r.lab.id = :labId " +
           "AND r.startTime <= :now AND r.endTime > :now " +
           "AND r.status = :status")
    List<Reservation> findCurrentReservations(
            @Param("labId") Integer labId,
            @Param("now") OffsetDateTime now,
            @Param("status") ReservationStatus status);

    /**
     * Find all reservations in a recurring group.
     */
    List<Reservation> findByRecurringGroupId(UUID recurringGroupId);

    /**
     * Find pending reservations for labs managed by a user.
     */
    @Query("SELECT r FROM Reservation r WHERE r.lab.id IN " +
           "(SELECT lm.lab.id FROM LabManager lm WHERE lm.user.id = :userId) " +
           "AND r.status = 'PENDING' " +
           "ORDER BY r.createdAt")
    List<Reservation> findPendingReservationsForManager(@Param("userId") Integer userId);

    /**
     * Find user's reservations with optional status filter.
     */
    List<Reservation> findByUserIdAndStatus(Integer userId, ReservationStatus status);

    /**
     * Count pending reservations for a lab.
     */
    int countByLabIdAndStatus(Integer labId, ReservationStatus status);
}
