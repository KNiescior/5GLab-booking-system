package com._glab.booking_system.booking.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReservationsResponse {

    /**
     * List of all pending reservations.
     */
    private List<ReservationResponse> reservations;

    /**
     * Map of recurring group IDs to their reservation counts.
     * Only includes reservations that are part of a recurring group.
     * Key: recurringGroupId, Value: number of occurrences in this group
     */
    private Map<UUID, Integer> recurringGroupCounts;

    /**
     * Total count of pending reservations.
     */
    private Integer totalCount;
}
