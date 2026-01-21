package com._glab.booking_system.booking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveReservationRequest {

    /**
     * Optional reason for approval.
     */
    private String reason;
}
