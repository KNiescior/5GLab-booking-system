package com._glab.booking_system.booking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectEditRequest {

    /**
     * Optional reason for rejecting the edit proposal.
     */
    private String reason;
}
