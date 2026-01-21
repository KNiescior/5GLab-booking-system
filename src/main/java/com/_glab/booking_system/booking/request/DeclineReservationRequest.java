package com._glab.booking_system.booking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclineReservationRequest {

    /**
     * Optional reason for decline/rejection.
     */
    private String reason;
}
