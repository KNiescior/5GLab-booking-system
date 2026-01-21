package com._glab.booking_system.booking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkReservationActionRequest {

    /**
     * Optional reason for the action (approve/decline).
     */
    private String reason;
}
