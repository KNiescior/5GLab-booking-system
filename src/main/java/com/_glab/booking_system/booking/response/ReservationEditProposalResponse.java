package com._glab.booking_system.booking.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com._glab.booking_system.booking.model.ReservationStatus;
import com._glab.booking_system.booking.model.ResolutionStatus;
import com._glab.booking_system.user.model.RoleName;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationEditProposalResponse {

    private UUID id;
    private UUID reservationId;

    /**
     * Information about who made the edit.
     */
    private EditorInfo editedBy;

    /**
     * Original status before the edit (PENDING or APPROVED).
     */
    private ReservationStatus originalStatus;

    /**
     * Original values before the edit.
     */
    private ReservationValues original;

    /**
     * Proposed new values.
     */
    private ReservationValues proposed;

    /**
     * When the edit proposal was created.
     */
    private OffsetDateTime createdAt;

    /**
     * Resolution status: PENDING, APPROVED, or REJECTED.
     */
    private ResolutionStatus resolution;

    /**
     * When the edit was resolved (approved/rejected).
     */
    private OffsetDateTime resolvedAt;

    /**
     * Who resolved the edit (if resolved).
     */
    private EditorInfo resolvedBy;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditorInfo {
        private Integer id;
        private String email;
        private String firstName;
        private String lastName;
        private RoleName role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationValues {
        private OffsetDateTime startTime;
        private OffsetDateTime endTime;
        private String description;
        private Boolean wholeLab;
        private List<Integer> workstationIds;
    }
}
