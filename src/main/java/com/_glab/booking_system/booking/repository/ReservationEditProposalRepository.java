package com._glab.booking_system.booking.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com._glab.booking_system.booking.model.ReservationEditProposal;
import com._glab.booking_system.booking.model.ResolutionStatus;

public interface ReservationEditProposalRepository extends JpaRepository<ReservationEditProposal, UUID> {

    /**
     * Find active (PENDING) edit proposal for a reservation.
     */
    Optional<ReservationEditProposal> findByReservationIdAndResolution(
            UUID reservationId, 
            ResolutionStatus resolution);

    /**
     * Find all edit proposals for a reservation (for history).
     */
    List<ReservationEditProposal> findByReservationId(UUID reservationId);

    /**
     * Find all active edit proposals for reservations in a recurring group.
     */
    @Query("SELECT ep FROM ReservationEditProposal ep " +
           "WHERE ep.reservation.recurringGroupId = :recurringGroupId " +
           "AND ep.resolution = :resolution")
    List<ReservationEditProposal> findByRecurringGroupIdAndResolution(
            @Param("recurringGroupId") UUID recurringGroupId,
            @Param("resolution") ResolutionStatus resolution);

    /**
     * Find all edit proposals for reservations in a recurring group (for history).
     */
    @Query("SELECT ep FROM ReservationEditProposal ep " +
           "WHERE ep.reservation.recurringGroupId = :recurringGroupId")
    List<ReservationEditProposal> findByRecurringGroupId(@Param("recurringGroupId") UUID recurringGroupId);
}
