package com._glab.booking_system.booking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabAvailabilityResponse {

    private Integer labId;
    private String labName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<OperatingHoursResponse> operatingHours;
    private List<ClosedDayResponse> closedDays;
    private List<ReservationSummaryResponse> reservations;
}

















