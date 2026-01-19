package com._glab.booking_system.booking.repository;

import com._glab.booking_system.booking.model.Reservation;
import com._glab.booking_system.booking.model.ReservationWorkstation;
import com._glab.booking_system.booking.model.Workstation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationWorkstationRepository extends JpaRepository<ReservationWorkstation, Integer> {

    List<ReservationWorkstation> findByReservation(Reservation reservation);

    List<ReservationWorkstation> findByReservationId(UUID reservationId);

    List<ReservationWorkstation> findByWorkstation(Workstation workstation);

    List<ReservationWorkstation> findByWorkstationId(Integer workstationId);

    /**
     * Find all workstation IDs for a reservation.
     */
    @Query("SELECT rw.workstation.id FROM ReservationWorkstation rw WHERE rw.reservation.id = :reservationId")
    List<Integer> findWorkstationIdsByReservationId(@Param("reservationId") UUID reservationId);

    /**
     * Delete all workstation assignments for a reservation.
     */
    void deleteByReservationId(UUID reservationId);
}
