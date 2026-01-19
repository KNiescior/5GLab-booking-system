package com._glab.booking_system.booking.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentAvailabilityResponse {

    private Integer labId;
    private String labName;
    private Boolean isOpen;
    private List<ReservationSummaryResponse> currentReservations;
}
