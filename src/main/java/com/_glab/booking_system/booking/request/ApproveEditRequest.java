package com._glab.booking_system.booking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveEditRequest {

    /**
     * Whether to approve (true) or reject (false) the edit proposal.
     */
    @Builder.Default
    private Boolean approve = true;
}
